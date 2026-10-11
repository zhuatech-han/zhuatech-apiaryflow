#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""独立本机TEST业务与持久化核验，状态含凭据不可提交。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse
import concurrent.futures
import datetime
import hashlib
import http.cookiejar
import json
import os
import secrets
import urllib.error
import urllib.request
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
CHECKS = 0


def check(condition, message):
    """核对真实结果，失败立即停止。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
    global CHECKS
    CHECKS += 1
    if not condition:
        raise AssertionError(message)


def private_write(path, value):
    """状态仅以0600保存到明确私有位置。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
    p = Path(path)
    fd = os.open(p, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w") as f:
        json.dump(value, f, ensure_ascii=False, indent=2)
    p.chmod(0o600)


class Client:
    """独立Cookie会话，全部写入获取当前CSRF，错误不自动重试。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""

    def __init__(self, base):
        self.base = base
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )

    def response(self, path, method="GET", body=None, csrf=True):
        headers = {}
        if method not in ["GET", "HEAD"] and csrf:
            token = self.call("/auth/csrf")
            headers[token["header"]] = token["token"]
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(
            self.base + "/api" + path, data=data, headers=headers, method=method
        )
        try:
            with self.opener.open(request, timeout=30) as r:
                return r.status, r.read(), r.headers.get("Content-Type", "")
        except urllib.error.HTTPError as e:
            return e.code, e.read(), e.headers.get("Content-Type", "")

    def call(self, path, method="GET", body=None):
        status, data, content_type = self.response(path, method, body)
        check(status == 200, f"{method} {path} returned {status}")
        if "json" in content_type:
            return json.loads(data)
        return data.decode("utf-8")

    def fail(self, path, method, body, status, code):
        actual, data, _ = self.response(path, method, body)
        check(actual == status, f"Expected {status} for {path}, got {actual}")
        check(json.loads(data)["code"] == code, f"Wrong error for {path}")

    def login(self, username, password):
        return self.call(
            "/auth/login", "POST", {"username": username, "password": password}
        )


def seed(base, state_path, env_file):
    """在独立空库创建TEST账号及完整复核、分群、转场与修订事实。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
    env = dict(
        line.split("=", 1)
        for line in Path(env_file).read_text().splitlines()
        if line and not line.startswith("#") and "=" in line
    )
    root = Client(base)
    root.login(env.get("ADMIN_USERNAME", "admin"), env["ADMIN_PASSWORD"])
    check(root.call("/colonies") == [], "Seed requires an empty TEST database")
    pw = "Aa9" + secrets.token_hex(20)
    clients, identities = {}, {}
    for name, role in [("keeper", 3), ("reviewer", 4), ("receiver", 3), ("reader", 5)]:
        user = root.call(
            "/admin/users",
            "POST",
            {
                "username": "test_" + name,
                "displayName": "TEST " + name,
                "password": pw,
                "roleId": role,
                "departmentId": 1,
                "enabled": True,
            },
        )
        identities[name] = user["id"]
        clients[name] = Client(base)
        clients[name].login("test_" + name, pw)
    keeper, reviewer, receiver, reader = [
        clients[k] for k in ["keeper", "reviewer", "receiver", "reader"]
    ]
    today = datetime.datetime.now(ZoneInfo("Asia/Shanghai")).date().isoformat()

    def site(code, name, capacity):
        return root.call(
            "/apiaries",
            "POST",
            {
                "code": code,
                "name": name,
                "departmentId": 1,
                "location": "TEST virtual site",
                "capacity": capacity,
                "enabled": True,
            },
        )

    origin = site("TEST_ORCHARD", "TEST 果园蜂场", 10)
    destination = site("TEST_MEADOW", "TEST 草地蜂场", 1)

    def create_colony(code, who):
        return root.call(
            "/colonies",
            "POST",
            {
                "code": code,
                "name": "TEST " + code,
                "apiaryId": origin["id"],
                "keeperId": identities[who],
                "category": "MELLIFERA",
                "source": "TEST learning registration",
                "openedOn": today,
            },
        )

    colony = create_colony("TEST_BEE01", "keeper")
    other = create_colony("TEST_BEE02", "receiver")

    def cdetail():
        return root.call(f"/colonies/{colony['id']}")["colony"]

    def obs():
        return {
            "colonyId": colony["id"],
            "observedOn": today,
            "coveredFrames": 8,
            "broodFrames": 5,
            "queenSeen": True,
            "stores": "NORMAL",
            "broodPattern": "EVEN",
            "observations": "TEST 现场观察\n仅记录事实，无处方",
            "reviewerId": identities["reviewer"],
        }

    reader.fail("/inspections", "POST", obs(), 403, "FORBIDDEN")
    keeper.fail(f"/colonies/{other['id']}", "GET", None, 403, "OUT_OF_SCOPE")
    check(
        [c["id"] for c in keeper.call("/colonies")] == [colony["id"]],
        "Assigned scope leaked another colony",
    )
    status, _, _ = root.response("/colonies", "POST", {}, csrf=False)
    check(status == 403, "CSRF not enforced")
    inspection = keeper.call("/inspections", "POST", obs())
    path = f"/inspections/{inspection['id']}"
    inspection = keeper.call(
        path + "/submit", "POST", {"version": inspection["version"]}
    )
    root.fail(
        path + "/verify",
        "POST",
        {"version": inspection["version"], "note": "TEST wrong actor"},
        409,
        "REVIEWER_ONLY",
    )
    inspection = reviewer.call(
        path + "/return",
        "POST",
        {"version": inspection["version"], "note": "TEST clarify counts"},
    )
    inspection = keeper.call(
        path,
        "PUT",
        {
            **obs(),
            "version": inspection["version"],
            "observations": "TEST 复核补充后现场记录\n保留实际框数",
        },
    )
    inspection = keeper.call(
        path + "/submit", "POST", {"version": inspection["version"]}
    )
    inspection = reviewer.call(
        path + "/verify",
        "POST",
        {"version": inspection["version"], "note": "TEST independent confirmation"},
    )
    check(inspection["state"] == "VERIFIED", "Inspection was not verified")
    old_digest, old_context = inspection["digest"], inspection["snapshotJson"]
    keeper.fail(
        path, "PUT", {**obs(), "version": inspection["version"]}, 409, "STATE_INVALID"
    )
    keeper.fail(path, "DELETE", {"version": inspection["version"]}, 409, "REFERENCED")
    original_csv = keeper.call(path + "/export.csv")
    check(old_digest in original_csv, "CSV omitted frozen inspection")
    colony = root.call(
        f"/colonies/{colony['id']}/requeen",
        "POST",
        {
            "version": cdetail()["version"],
            "happenedOn": today,
            "queenCode": "TEST_Q2026",
            "note": "TEST observed queen replacement",
        },
    )
    colony = root.call(
        f"/colonies/{colony['id']}/split",
        "POST",
        {
            "version": colony["version"],
            "happenedOn": today,
            "code": "TEST_DAUGHTER",
            "name": "TEST 分出蜂群",
            "note": "TEST factual daughter colony registration",
        },
    )
    detail = root.call(f"/colonies/{colony['id']}")
    child = detail["children"][0]
    check(
        child["parentId"] == colony["id"] and colony["state"] == "ACTIVE",
        "Split identity incorrect",
    )
    body = {
        "code": "TEST_MOVE01",
        "sourceId": origin["id"],
        "destinationId": destination["id"],
        "senderId": identities["keeper"],
        "receiverId": identities["receiver"],
        "plannedOn": today,
        "notes": "TEST orchard to meadow relocation",
        "colonyIds": [colony["id"], child["id"]],
    }
    move = root.call("/transfers", "POST", body)
    mpath = f"/transfers/{move['id']}"
    root.fail(
        mpath + "/release",
        "POST",
        {"version": move["version"]},
        409,
        "CAPACITY_EXCEEDED",
    )
    destination = root.call(
        f"/apiaries/{destination['id']}", "PUT", {**destination, "capacity": 3}
    )
    move = root.call(mpath + "/release", "POST", {"version": move["version"]})
    frozen_manifest = move["snapshotJson"]
    keeper.fail("/inspections", "POST", obs(), 409, "COLONY_BUSY")
    receiver.fail(
        mpath + "/depart",
        "POST",
        {
            "version": move["version"],
            "happenedOn": today,
            "note": "TEST wrong departure actor",
        },
        409,
        "OPERATOR_ONLY",
    )
    receiver.fail(
        mpath + "/receive",
        "POST",
        {
            "version": move["version"],
            "happenedOn": today,
            "note": "TEST premature receipt",
        },
        409,
        "STATE_INVALID",
    )
    move = keeper.call(
        mpath + "/depart",
        "POST",
        {
            "version": move["version"],
            "happenedOn": today,
            "note": "TEST boxes counted before departure",
        },
    )
    check(
        cdetail()["state"] == "IN_TRANSIT" and cdetail()["apiaryId"] == origin["id"],
        "Premature location mutation",
    )
    root.fail(
        mpath + "/cancel",
        "POST",
        {"version": move["version"], "note": "TEST cannot cancel physical departure"},
        409,
        "STATE_INVALID",
    )
    move = receiver.call(
        mpath + "/receive",
        "POST",
        {
            "version": move["version"],
            "happenedOn": today,
            "note": "TEST both colonies physically received",
        },
    )
    check(
        move["state"] == "RECEIVED" and move["snapshotJson"] == frozen_manifest,
        "Manifest overwritten",
    )
    check(
        cdetail()["apiaryId"] == destination["id"] and cdetail()["moveId"] is None,
        "Receipt did not settle location/reservation",
    )
    previous = keeper.call(path)["record"]
    check(
        previous["digest"] == old_digest and previous["snapshotJson"] == old_context,
        "Historic inspection mutated after queen/transfer",
    )
    correction = keeper.call(
        "/inspections",
        "POST",
        {
            **obs(),
            "supersedesId": inspection["id"],
            "broodFrames": 4,
            "observations": "TEST correction of original site observation",
        },
    )
    ipath = f"/inspections/{correction['id']}"
    correction = keeper.call(
        ipath + "/submit", "POST", {"version": correction["version"]}
    )
    check(
        json.loads(correction["snapshotJson"])["apiaryId"] == origin["id"],
        "Correction used current site instead of historic observation site",
    )
    correction = reviewer.call(
        ipath + "/verify",
        "POST",
        {
            "version": correction["version"],
            "note": "TEST independent correction verified",
        },
    )
    check(keeper.call(path)["superseded"] is True, "Original not linked to replacement")
    check(keeper.call(path + "/export.csv") == original_csv, "Original CSV changed")
    actual_child = root.call(f"/colonies/{child['id']}")["colony"]
    root.call(
        f"/colonies/{child['id']}/retire",
        "POST",
        {
            "version": actual_child["version"],
            "happenedOn": today,
            "note": "TEST end of observation register",
        },
    )
    cancelled = root.call(
        "/transfers",
        "POST",
        {
            **body,
            "code": "TEST_MOVE02",
            "sourceId": destination["id"],
            "destinationId": origin["id"],
            "colonyIds": [colony["id"]],
        },
    )
    cp = f"/transfers/{cancelled['id']}"
    cancelled = root.call(cp + "/release", "POST", {"version": cancelled["version"]})
    cancelled = root.call(
        cp + "/cancel",
        "POST",
        {"version": cancelled["version"], "note": "TEST cancelled before departure"},
    )
    check(
        cancelled["state"] == "CANCELLED" and cdetail()["moveId"] is None,
        "Cancel failed to release reservation",
    )
    current = cdetail()
    a, b = Client(base), Client(base)
    a.login("admin", env["ADMIN_PASSWORD"])
    b.login("admin", env["ADMIN_PASSWORD"])
    edits = [
        {**current, "name": "TEST 一号蜂群 A"},
        {**current, "name": "TEST 一号蜂群 B"},
    ]
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        futures = [
            pool.submit(c.response, f"/colonies/{colony['id']}", "PUT", value)
            for c, value in zip([a, b], edits)
        ]
        statuses = sorted(f.result()[0] for f in futures)
    check(
        statuses == [200, 409], "Concurrent version checks must return exactly 200/409"
    )
    check(
        root.call("/reports")["received"] == 1, "Received transfer statistic incorrect"
    )
    check(
        root.call("/reports")["verified"] == 1, "Superseded record counted as current"
    )
    d = root.call(
        "/admin/departments",
        "POST",
        {"name": "TEST separate team", "zone": "UTC", "enabled": True},
    )
    remote = root.call(
        "/admin/users",
        "POST",
        {
            "username": "test_remote",
            "displayName": "TEST remote",
            "password": pw,
            "roleId": 3,
            "departmentId": d["id"],
            "enabled": True,
        },
    )
    remote_site = root.call(
        "/apiaries",
        "POST",
        {
            "code": "TEST_REMOTE",
            "name": "TEST 独立团队蜂场",
            "departmentId": d["id"],
            "location": "TEST remote virtual site",
            "capacity": 5,
            "enabled": True,
        },
    )
    remote_colony = root.call(
        "/colonies",
        "POST",
        {
            "code": "TEST_REMOTE_BEE",
            "name": "TEST remote colony",
            "apiaryId": remote_site["id"],
            "keeperId": remote["id"],
            "category": "UNKNOWN",
            "source": "TEST separate scope",
            "openedOn": today,
        },
    )
    keeper.fail(f"/colonies/{remote_colony['id']}", "GET", None, 403, "OUT_OF_SCOPE")
    user = next(u for u in root.call("/admin/users") if u["id"] == remote["id"])
    remote_client = Client(base)
    remote_client.login("test_remote", pw)
    root.call(f"/admin/users/{user['id']}", "PUT", {**user, "enabled": False})
    remote_client.fail("/auth/me", "GET", None, 401, "UNAUTHENTICATED")
    state = {
        "adminUsername": env.get("ADMIN_USERNAME", "admin"),
        "adminPassword": env["ADMIN_PASSWORD"],
        "staffPassword": pw,
        "people": identities,
        "colonyId": colony["id"],
        "childId": child["id"],
        "inspectionId": inspection["id"],
        "correctionId": correction["id"],
        "transferId": move["id"],
        "cancelledId": cancelled["id"],
    }
    private_write(state_path, state)
    print(
        json.dumps(
            {
                "mode": "seed",
                "checks": CHECKS,
                "result": "passed",
                "privateState": "created; credentials omitted",
            }
        )
    )


def capture(base, state):
    """采集全部目录、业务、事件和导出；仅忽略重新登录产生的LOGIN审计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
    client = Client(base)
    client.login(state["adminUsername"], state["adminPassword"])
    data = {}
    for k in [
        "users",
        "roles",
        "permissions",
        "departments",
        "menus",
        "dictionaries",
        "settings",
    ]:
        data["admin/" + k] = client.call("/admin/" + k)
    for k in ["apiaries", "colonies", "inspections", "transfers", "reports"]:
        data[k] = client.call("/" + k)
    for c in data["colonies"]:
        data["colony/" + str(c["id"])] = client.call(f"/colonies/{c['id']}")
    for k in ["inspections", "transfers"]:
        for record in data[k]:
            rid = record["id"]
            data[f"{k}/{rid}"] = client.call(f"/{k}/{rid}")
            if (k == "inspections" and record["state"] == "VERIFIED") or (
                k == "transfers" and record["snapshotJson"]
            ):
                csv = client.call(f"/{k}/{rid}/export.csv")
                data[f"{k}/{rid}/csv"] = csv
                check(
                    record["digest"] in csv
                    and hashlib.sha256(record["snapshotJson"].encode()).hexdigest()
                    == record["digest"],
                    "Frozen snapshot digest or CSV content mismatch",
                )
    data["audit"] = [a for a in client.call("/audit") if a["action"] != "LOGIN"]
    return data


def main():
    """显式指定隔离本机实例和私有文件，不允许对生产地址播种。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
    p = argparse.ArgumentParser(
        description="Isolated localhost TEST database only. State includes private credentials."
    )
    p.add_argument("--base", default="http://127.0.0.1:19137")
    p.add_argument("--env-file", default=str(ROOT / ".env"))
    p.add_argument("--state", required=True)
    p.add_argument("--mode", choices=["seed", "capture", "compare"], default="compare")
    p.add_argument("--snapshot")
    a = p.parse_args()
    if not a.base.startswith(("http://127.0.0.1:", "http://localhost:")):
        p.error("Only isolated localhost TEST instances are allowed")
    if a.mode == "seed":
        seed(a.base, a.state, a.env_file)
        return
    if not a.snapshot:
        p.error("--snapshot required")
    state = json.loads(Path(a.state).read_text())
    current = capture(a.base, state)
    if a.mode == "capture":
        private_write(a.snapshot, current)
    else:
        check(
            current == json.loads(Path(a.snapshot).read_text()),
            "Persisted directories, business, events, non-LOGIN audit and CSV differ from baseline",
        )
    print(
        json.dumps(
            {"mode": a.mode, "checks": CHECKS, "sets": len(current), "result": "passed"}
        )
    )


if __name__ == "__main__":
    main()
