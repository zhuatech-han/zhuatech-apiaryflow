// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.apiaryflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 蜂群记录与转场的真实事务边界；只记录事实，不输出养殖或诊疗建议。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional
public class ApiaryService {
  final Store db;
  final AccessService access;
  final AdminService admin;
  final Clock clock;
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  /** 连接持久化、实时身份和团队时钟。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public ApiaryService(Store db, AccessService access, AdminService admin, Clock clock) {
    this.db = db;
    this.access = access;
    this.admin = admin;
    this.clock = clock;
  }

  void version(long v, Map<String, Object> b) {
    Rules.check(v == Rules.id(b.get("version")), "VERSION_CONFLICT");
  }

  void allowed(boolean ok) {
    if (!ok) throw new Problem(403, "OUT_OF_SCOPE");
  }

  String code(Object o) {
    var s = Rules.text(o, 30, true);
    Rules.check(s.matches("[A-Z0-9_]{2,30}"), "CODE_INVALID");
    return s;
  }

  String note(Map<String, Object> b) {
    var n = Rules.paragraph(b.get("note"), 500, true);
    Rules.check(n.length() >= 4, "NOTE_REQUIRED");
    return n;
  }

  boolean assigned() {
    return access.role().scope.equals("ASSIGNED");
  }

  boolean colonyVisible(Colony c) {
    return access.department(c.departmentId)
        && (!assigned()
            || Objects.equals(c.keeperId, access.current().id)
            || db.query(
                        Inspection.class,
                        "from Inspection where colonyId=?1 and reviewerId=?2",
                        c.id,
                        access.current().id)
                    .size()
                > 0);
  }

  boolean transferVisible(Transfer x) {
    var id = access.current().id;
    return access.department(x.departmentId)
        && (!assigned()
            || Objects.equals(x.createdBy, id)
            || Objects.equals(x.senderId, id)
            || Objects.equals(x.receiverId, id));
  }

  boolean inspectionVisible(Inspection x) {
    var id = access.current().id;
    return access.department(x.departmentId)
        && (!assigned() || Objects.equals(x.inspectorId, id) || Objects.equals(x.reviewerId, id));
  }

  Colony colony(Long id) {
    var c = db.get(Colony.class, id);
    allowed(colonyVisible(c));
    return c;
  }

  Transfer transfer(Long id) {
    var x = db.get(Transfer.class, id);
    allowed(transferVisible(x));
    return x;
  }

  LocalDate today(Long dept) {
    return LocalDate.now(clock.withZone(ZoneId.of(db.get(Department.class, dept).zone)));
  }

  LocalDate date(Object o, Long dept, boolean future) {
    try {
      var d = LocalDate.parse(Rules.text(o, 10, true));
      Rules.check(
          d.getYear() >= 2000 && d.getYear() <= 2100 && (future || !d.isAfter(today(dept))),
          "DATE_INVALID");
      return d;
    } catch (java.time.format.DateTimeParseException e) {
      throw new Problem(400, "INVALID_INPUT");
    }
  }

  void active(Colony c) {
    Rules.check(c.state.equals("ACTIVE") && c.moveId == null, "COLONY_BUSY");
  }

  void chronological(Colony c, LocalDate d) {
    Rules.check(!d.isBefore(c.lastOn), "DATE_ORDER");
  }

  void noPending(Colony c) {
    Rules.check(
        db.query(
                Inspection.class, "from Inspection where colonyId=?1 and state=?2", c.id, "PENDING")
            .isEmpty(),
        "INSPECTION_PENDING");
  }

  Account worker(Long id, String permission, Long dept) {
    var a = db.get(Account.class, id);
    Rules.check(
        a.enabled
            && Objects.equals(a.departmentId, dept)
            && db.get(Department.class, dept).enabled
            && db.get(AccessRole.class, a.roleId).permissions.contains(permission),
        "WORKER_UNAVAILABLE");
    return a;
  }

  ApiarySite site(Long id) {
    return db.get(ApiarySite.class, id);
  }

  String encode(Object v) {
    try {
      return json.writeValueAsString(v);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  Map<String, Object> originalContext(Inspection x, Colony c) {
    if (x.supersedesId == null) return identity(c);
    var previous = db.get(Inspection.class, x.supersedesId);
    hash(previous.snapshotJson, previous.digest);
    Rules.check(previous.inspectorId.equals(x.inspectorId), "REVISION_INVALID");
    try {
      return json.readValue(
          previous.snapshotJson,
          new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  String digest(String s) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  void hash(String data, String d) {
    Rules.check(digest(data).equals(d), "SNAPSHOT_CHANGED");
  }

  Map<String, Object> identity(Colony c) {
    var s = site(c.apiaryId);
    var m = new LinkedHashMap<String, Object>();
    m.put("id", c.id);
    m.put("code", c.code);
    m.put("name", c.name);
    m.put("apiaryId", s.id);
    m.put("apiaryCode", s.code);
    m.put("apiaryName", s.name);
    m.put("keeperId", c.keeperId);
    m.put("queenCode", c.queenCode);
    m.put("parentId", c.parentId);
    return m;
  }

  void event(Colony c, String action, LocalDate day, String notes, Object payload) {
    var e = new ColonyEvent();
    e.colonyId = c.id;
    e.departmentId = c.departmentId;
    e.actorId = access.current().id;
    e.action = action;
    e.happenedOn = day;
    e.notes = notes;
    e.snapshotJson = encode(payload);
    e.createdAt = clock.instant();
    db.save(e);
    access.audit(action, c.id, c.departmentId);
  }

  void capacity(ApiarySite s, int additional) {
    long residents =
        db.query(Colony.class, "from Colony where apiaryId=?1 and state<>?2", s.id, "RETIRED")
            .size();
    long incoming =
        db
            .query(
                Transfer.class,
                "from Transfer where destinationId=?1 and state in (?2,?3)",
                s.id,
                "RELEASED",
                "IN_TRANSIT")
            .stream()
            .mapToLong(x -> x.colonyIds.size())
            .sum();
    Rules.check(residents + incoming + additional <= s.capacity, "CAPACITY_EXCEEDED");
  }

  /** 读取授权列表；ASSIGNED仅本人蜂群、巡检和指定交接。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object read(String kind) {
    return switch (kind) {
      case "apiaries" -> {
        access.require("apiaries");
        var ids = new HashSet<Long>();
        if (assigned()) {
          db.all(Colony.class).stream()
              .filter(this::colonyVisible)
              .forEach(c -> ids.add(c.apiaryId));
          db.all(Transfer.class).stream()
              .filter(this::transferVisible)
              .forEach(
                  x -> {
                    ids.add(x.sourceId);
                    ids.add(x.destinationId);
                  });
        }
        yield db.all(ApiarySite.class).stream()
            .filter(s -> access.department(s.departmentId) && (!assigned() || ids.contains(s.id)))
            .toList();
      }
      case "colonies" -> {
        access.require("colonies");
        yield db.all(Colony.class).stream().filter(this::colonyVisible).toList();
      }
      case "inspections" -> {
        access.require("colonies");
        yield db.all(Inspection.class).stream().filter(this::inspectionVisible).toList();
      }
      case "transfers" -> {
        access.require("transfers");
        yield db.all(Transfer.class).stream().filter(this::transferVisible).toList();
      }
      case "options" -> options();
      case "reports" -> report();
      case "audit" -> {
        access.require("audit");
        yield db.all(AuditEvent.class).stream()
            .filter(
                x ->
                    access.department(x.departmentId)
                        && (!assigned() || x.actor.equals(access.current().username)))
            .sorted(Comparator.comparing((AuditEvent e) -> e.id).reversed())
            .limit(200)
            .toList();
      }
      default -> throw new Problem(404, "NOT_FOUND");
    };
  }

  /** 表单候选人仅返回身份显示及岗位能力，不枚举密码或跨团队个人资料。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object options() {
    access.current();
    var people =
        db.all(Account.class).stream()
            .filter(
                a ->
                    a.enabled
                        && access.department(a.departmentId)
                        && db.get(Department.class, a.departmentId).enabled)
            .map(
                a ->
                    Map.of(
                        "id",
                        a.id,
                        "displayName",
                        a.displayName,
                        "departmentId",
                        a.departmentId,
                        "permissions",
                        db.get(AccessRole.class, a.roleId).permissions))
            .toList();
    var sites =
        db.all(ApiarySite.class).stream()
            .filter(s -> access.department(s.departmentId))
            .map(
                s ->
                    Map.of(
                        "id",
                        s.id,
                        "code",
                        s.code,
                        "name",
                        s.name,
                        "departmentId",
                        s.departmentId,
                        "enabled",
                        s.enabled))
            .toList();
    return Map.of("people", people, "apiaries", sites, "categories", db.all(DictionaryEntry.class));
  }

  /** 蜂场容量调整不能低于居民及已发布接收预留；历史身份不可变。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object saveSite(Long id, Map<String, Object> b) {
    admin.lock();
    access.require("manage");
    allowed(!assigned());
    var s = id == null ? new ApiarySite() : site(id);
    if (id == null) {
      s.departmentId = Rules.id(b.get("departmentId"));
      allowed(access.department(s.departmentId));
      Rules.check(db.get(Department.class, s.departmentId).enabled, "DEPARTMENT_DISABLED");
      s.code = code(b.get("code"));
    } else {
      allowed(access.department(s.departmentId));
      version(s.version, b);
      Rules.check(
          s.code.equals(b.get("code"))
              && Objects.equals(s.departmentId, Rules.id(b.get("departmentId"))),
          "IDENTITY_LOCKED");
    }
    s.name = Rules.text(b.get("name"), 120, true);
    s.location = Rules.text(b.get("location"), 300, true);
    s.capacity = Rules.integer(b.get("capacity"), 1, 1000);
    s.enabled = Rules.flag(b.get("enabled"));
    if (id != null) {
      capacity(s, 0);
      if (!s.enabled)
        Rules.check(
            db.query(Colony.class, "from Colony where apiaryId=?1 and state<>?2", id, "RETIRED")
                    .isEmpty()
                && db.query(
                        Transfer.class,
                        "from Transfer where (sourceId=?1 or destinationId=?1) and state in (?2,?3)",
                        id,
                        "RELEASED",
                        "IN_TRANSIT")
                    .isEmpty(),
            "REFERENCED");
      s.version++;
    } else {
      Rules.check(db.all(ApiarySite.class).size() < 100, "RESOURCE_LIMIT");
      db.save(s);
    }
    access.audit("APIARY_SAVE", s.id, s.departmentId);
    return s;
  }

  /** 建档或维护蜂群，位置只能通过交接改变，历史账号与编号保持。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object saveColony(Long id, Map<String, Object> b) {
    admin.lock();
    access.require("manage");
    allowed(!assigned());
    var c = id == null ? new Colony() : colony(id);
    if (id == null) {
      var s = site(Rules.id(b.get("apiaryId")));
      allowed(access.department(s.departmentId));
      Rules.check(s.enabled, "SITE_DISABLED");
      capacity(s, 1);
      Rules.check(db.all(Colony.class).size() < admin.setting("max_colonies"), "RESOURCE_LIMIT");
      c.code = code(b.get("code"));
      c.apiaryId = s.id;
      c.departmentId = s.departmentId;
      c.createdBy = access.current().id;
      c.openedOn = date(b.get("openedOn"), c.departmentId, false);
      c.lastOn = c.openedOn;
      c.state = "ACTIVE";
      c.source = Rules.text(b.get("source"), 300, true);
    } else {
      version(c.version, b);
      active(c);
      noPending(c);
      Rules.check(
          c.code.equals(b.get("code")) && Objects.equals(c.apiaryId, Rules.id(b.get("apiaryId"))),
          "IDENTITY_LOCKED");
    }
    c.name = Rules.text(b.get("name"), 120, true);
    var keeper = worker(Rules.id(b.get("keeperId")), "inspect", c.departmentId);
    if (assigned()) allowed(Objects.equals(keeper.id, access.current().id));
    if (id != null && !Objects.equals(c.keeperId, keeper.id))
      Rules.check(
          db.query(
                  Inspection.class,
                  "from Inspection where colonyId=?1 and state in (?2,?3)",
                  c.id,
                  "DRAFT",
                  "PENDING")
              .isEmpty(),
          "INSPECTION_PENDING");
    c.keeperId = keeper.id;
    String cat = Rules.text(b.get("category"), 60, true);
    Rules.check(
        db.query(
                    DictionaryEntry.class,
                    "from DictionaryEntry where type=?1 and code=?2 and enabled=true",
                    "SPECIES",
                    cat)
                .size()
            == 1,
        "CATEGORY_INVALID");
    c.category = cat;
    if (id == null) {
      db.save(c);
      event(c, "REGISTER", c.openedOn, c.source, identity(c));
    } else {
      c.version++;
      event(c, "DETAILS_UPDATE", today(c.departmentId), "", identity(c));
    }
    return c;
  }

  /** 仅从未使用的误建蜂群可删除；正常退出保留谱系与全部记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object deleteColony(Long id, Map<String, Object> b) {
    admin.lock();
    access.require("manage");
    allowed(!assigned());
    var c = colony(id);
    version(c.version, b);
    active(c);
    Rules.check(
        db.query(Inspection.class, "from Inspection where colonyId=?1", id).isEmpty()
            && db.query(Colony.class, "from Colony where parentId=?1", id).isEmpty()
            && db.query(ColonyEvent.class, "from ColonyEvent where colonyId=?1", id).size() == 1
            && db.all(Transfer.class).stream().noneMatch(x -> x.colonyIds.contains(id)),
        "REFERENCED");
    for (var e : db.query(ColonyEvent.class, "from ColonyEvent where colonyId=?1", id))
      db.delete(e);
    db.delete(c);
    access.audit("COLONY_DELETE", id, c.departmentId);
    return Map.of("ok", true);
  }

  /** 分群和换王追加独立事实；不将分群关系宣称为遗传鉴定。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object colonyAction(Long id, String action, Map<String, Object> b) {
    admin.lock();
    access.require("manage");
    allowed(!assigned());
    var c = colony(id);
    version(c.version, b);
    active(c);
    noPending(c);
    var day = date(b.get("happenedOn"), c.departmentId, false);
    chronological(c, day);
    var notes = note(b);
    var payload = new LinkedHashMap<>(identity(c));
    switch (action) {
      case "split" -> {
        var s = site(c.apiaryId);
        Rules.check(s.enabled, "SITE_DISABLED");
        capacity(s, 1);
        Rules.check(db.all(Colony.class).size() < admin.setting("max_colonies"), "RESOURCE_LIMIT");
        var child = new Colony();
        child.code = code(b.get("code"));
        child.name = Rules.text(b.get("name"), 120, true);
        child.departmentId = c.departmentId;
        child.apiaryId = c.apiaryId;
        child.keeperId = c.keeperId;
        child.createdBy = access.current().id;
        child.category = c.category;
        child.source = notes;
        child.openedOn = day;
        child.lastOn = day;
        child.state = "ACTIVE";
        child.parentId = c.id;
        db.save(child);
        payload.put("child", identity(child));
        event(child, "SPLIT_FROM", day, notes, identity(c));
        event(c, "SPLIT_TO", day, notes, payload);
      }
      case "requeen" -> {
        String queen = Rules.text(b.get("queenCode"), 60, true);
        Rules.check(!queen.equals(c.queenCode), "IDENTITY_LOCKED");
        payload.put("oldQueen", c.queenCode);
        payload.put("newQueen", queen);
        c.queenCode = queen;
        c.queenSince = day;
        event(c, "REQUEEN", day, notes, payload);
      }
      case "retire" -> {
        c.state = "RETIRED";
        event(c, "RETIRE", day, notes, payload);
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    c.lastOn = day;
    c.version++;
    return c;
  }

  Inspection inspection(Long id) {
    var x = db.get(Inspection.class, id);
    allowed(inspectionVisible(x));
    return x;
  }

  Inspection latest(Long colonyId) {
    var rows =
        db.query(
            Inspection.class,
            "from Inspection where colonyId=?1 and state=?2 order by observedOn desc,id desc",
            colonyId,
            "VERIFIED");
    return rows.isEmpty() ? null : rows.getFirst();
  }

  /** 管护人只能编辑自己未送审的记录；修订必须指向尚未被代替的核实记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object saveInspection(Long id, Map<String, Object> b) {
    admin.lock();
    access.require("inspect");
    var x = id == null ? new Inspection() : inspection(id);
    var c = colony(Rules.id(b.get("colonyId")));
    active(c);
    Rules.check(Objects.equals(c.keeperId, access.current().id), "KEEPER_ONLY");
    worker(c.keeperId, "inspect", c.departmentId);
    if (id == null) {
      Rules.check(
          db.all(Inspection.class).size() < admin.setting("max_inspections"), "RESOURCE_LIMIT");
      x.colonyId = c.id;
      x.departmentId = c.departmentId;
      x.inspectorId = access.current().id;
      if (b.get("supersedesId") != null && !Objects.toString(b.get("supersedesId")).isBlank()) {
        var old = inspection(Rules.id(b.get("supersedesId")));
        Rules.check(
            old.colonyId.equals(c.id) && old.state.equals("VERIFIED") && !superseded(old.id),
            "REVISION_INVALID");
        x.supersedesId = old.id;
      }
      x.state = "DRAFT";
    } else {
      version(x.version, b);
      Rules.check(x.colonyId.equals(c.id), "IDENTITY_LOCKED");
      Rules.check(x.state.equals("DRAFT"), "STATE_INVALID");
      Rules.check(x.inspectorId.equals(access.current().id), "KEEPER_ONLY");
      x.version++;
    }
    x.observedOn = date(b.get("observedOn"), c.departmentId, false);
    if (x.supersedesId == null) chronological(c, x.observedOn);
    else
      Rules.check(
          x.observedOn.equals(db.get(Inspection.class, x.supersedesId).observedOn), "DATE_ORDER");
    x.coveredFrames = Rules.integer(b.get("coveredFrames"), 0, 200);
    x.broodFrames = Rules.integer(b.get("broodFrames"), 0, 200);
    Rules.check(x.broodFrames <= x.coveredFrames, "FRAME_INVALID");
    x.queenSeen = Rules.flag(b.get("queenSeen"));
    x.stores = Rules.text(b.get("stores"), 20, true);
    x.broodPattern = Rules.text(b.get("broodPattern"), 20, true);
    Rules.check(
        Set.of("LOW", "NORMAL", "HIGH", "UNKNOWN").contains(x.stores)
            && Set.of("EVEN", "PATCHY", "NONE", "UNKNOWN").contains(x.broodPattern),
        "INVALID_INPUT");
    x.observations = Rules.paragraph(b.get("observations"), 2000, true);
    x.reviewerId = Rules.id(b.get("reviewerId"));
    worker(x.reviewerId, "review", c.departmentId);
    Rules.check(!x.reviewerId.equals(x.inspectorId), "INDEPENDENT_REQUIRED");
    if (id == null) db.save(x);
    access.audit("INSPECTION_SAVE", x.id, c.departmentId);
    return x;
  }

  boolean superseded(Long id) {
    return !db.query(
            Inspection.class, "from Inspection where supersedesId=?1 and state=?2", id, "VERIFIED")
        .isEmpty();
  }

  /** 送审冻结现场身份和观察；独立核实后只通过新记录修订，绝不覆盖旧事实。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object inspectionAction(Long id, String action, Map<String, Object> b) {
    admin.lock();
    var x = inspection(id);
    version(x.version, b);
    var c = colony(x.colonyId);
    active(c);
    if (action.equals("submit") || action.equals("withdraw") || action.equals("cancel")) {
      access.require("inspect");
      Rules.check(
          x.inspectorId.equals(access.current().id) && c.keeperId.equals(access.current().id),
          "KEEPER_ONLY");
      worker(c.keeperId, "inspect", c.departmentId);
    }
    switch (action) {
      case "submit" -> {
        Rules.check(x.state.equals("DRAFT"), "STATE_INVALID");
        noPending(c);
        worker(x.reviewerId, "review", c.departmentId);
        Rules.check(!x.reviewerId.equals(x.inspectorId), "INDEPENDENT_REQUIRED");
        if (x.supersedesId == null) chronological(c, x.observedOn);
        else Rules.check(!superseded(x.supersedesId), "REVISION_INVALID");
        var snap = new LinkedHashMap<>(originalContext(x, c));
        snap.put("observedOn", x.observedOn.toString());
        snap.put("inspectorId", x.inspectorId);
        snap.put("reviewerId", x.reviewerId);
        snap.put("coveredFrames", x.coveredFrames);
        snap.put("broodFrames", x.broodFrames);
        snap.put("queenSeen", x.queenSeen);
        snap.put("stores", x.stores);
        snap.put("broodPattern", x.broodPattern);
        snap.put("observations", x.observations);
        snap.put("supersedesId", x.supersedesId);
        x.snapshotJson = encode(snap);
        x.digest = digest(x.snapshotJson);
        x.state = "PENDING";
        x.decisionNote = "";
      }
      case "withdraw" -> {
        Rules.check(x.state.equals("PENDING"), "STATE_INVALID");
        x.state = "DRAFT";
        x.decisionNote = note(b);
      }
      case "cancel" -> {
        Rules.check(x.state.equals("DRAFT"), "STATE_INVALID");
        x.state = "CANCELLED";
        x.decisionNote = note(b);
      }
      case "verify", "return" -> {
        access.require("review");
        Rules.check(x.state.equals("PENDING"), "STATE_INVALID");
        Rules.check(access.current().id.equals(x.reviewerId), "REVIEWER_ONLY");
        worker(x.reviewerId, "review", x.departmentId);
        Rules.check(!x.inspectorId.equals(x.reviewerId), "INDEPENDENT_REQUIRED");
        hash(x.snapshotJson, x.digest);
        x.decisionNote = note(b);
        if (action.equals("verify")) {
          worker(x.inspectorId, "inspect", x.departmentId);
          Rules.check(x.inspectorId.equals(c.keeperId), "KEEPER_ONLY");
          if (x.supersedesId == null) {
            chronological(c, x.observedOn);
            c.lastOn = x.observedOn;
          } else Rules.check(!superseded(x.supersedesId), "REVISION_INVALID");
          c.version++;
          x.state = "VERIFIED";
          event(
              c,
              "INSPECTION_VERIFY",
              x.observedOn,
              x.decisionNote,
              Map.of("inspectionId", x.id, "digest", x.digest));
        } else x.state = "DRAFT";
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    x.version++;
    access.audit("INSPECTION_" + action.toUpperCase(Locale.ROOT), x.id, x.departmentId);
    return x;
  }

  /** 仅本人草稿可删除，送审或核实记录不提供删除路径。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object deleteInspection(Long id, Map<String, Object> b) {
    admin.lock();
    access.require("inspect");
    var x = inspection(id);
    version(x.version, b);
    Rules.check(x.inspectorId.equals(access.current().id), "KEEPER_ONLY");
    Rules.check(x.state.equals("DRAFT") && x.snapshotJson.isEmpty(), "REFERENCED");
    db.delete(x);
    access.audit("INSPECTION_DELETE", id, x.departmentId);
    return Map.of("ok", true);
  }

  /** 同团队转场草稿保存真实清单；指定移出人与接收人必须不同。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object saveTransfer(Long id, Map<String, Object> b) {
    admin.lock();
    access.require("plan");
    var x = id == null ? new Transfer() : transfer(id);
    if (id != null) {
      version(x.version, b);
      Rules.check(x.state.equals("DRAFT"), "STATE_INVALID");
      x.version++;
    }
    var source = site(Rules.id(b.get("sourceId")));
    var target = site(Rules.id(b.get("destinationId")));
    allowed(access.department(source.departmentId));
    Rules.check(
        source.enabled
            && target.enabled
            && source.departmentId.equals(target.departmentId)
            && !source.id.equals(target.id),
        "SITE_INVALID");
    if (id == null) {
      Rules.check(db.all(Transfer.class).size() < admin.setting("max_transfers"), "RESOURCE_LIMIT");
      x.code = code(b.get("code"));
      x.createdBy = access.current().id;
      x.departmentId = source.departmentId;
      x.state = "DRAFT";
    } else
      Rules.check(
          x.code.equals(b.get("code")) && x.departmentId.equals(source.departmentId),
          "IDENTITY_LOCKED");
    x.sourceId = source.id;
    x.destinationId = target.id;
    x.senderId = worker(Rules.id(b.get("senderId")), "handover", source.departmentId).id;
    x.receiverId = worker(Rules.id(b.get("receiverId")), "handover", source.departmentId).id;
    Rules.check(!x.senderId.equals(x.receiverId), "INDEPENDENT_REQUIRED");
    x.plannedOn = date(b.get("plannedOn"), source.departmentId, true);
    x.notes = Rules.paragraph(b.get("notes"), 500, true);
    if (!(b.get("colonyIds") instanceof List<?> ids) || ids.isEmpty() || ids.size() > 50)
      throw new Problem(400, "LINE_LIMIT");
    x.colonyIds.clear();
    for (var v : ids) {
      var c = colony(Rules.id(v));
      active(c);
      Rules.check(
          c.apiaryId.equals(source.id) && c.departmentId.equals(source.departmentId),
          "SITE_INVALID");
      Rules.check(x.colonyIds.add(c.id), "DUPLICATE_COLONY");
      chronological(c, x.plannedOn);
    }
    if (id == null) db.save(x);
    access.audit("TRANSFER_SAVE", x.id, x.departmentId);
    return x;
  }

  /** 发布锁住蜂群并预留目标容量；移出与接收分别由指定人员确认。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object transferAction(Long id, String action, Map<String, Object> b) {
    admin.lock();
    access.require("transfers");
    var x = transfer(id);
    version(x.version, b);
    var source = site(x.sourceId);
    var target = site(x.destinationId);
    if (action.equals("release") || action.equals("cancel")) access.require("plan");
    else {
      access.require("handover");
      Long operator = action.equals("depart") ? x.senderId : x.receiverId;
      Rules.check(access.current().id.equals(operator), "OPERATOR_ONLY");
      worker(operator, "handover", x.departmentId);
    }
    switch (action) {
      case "release" -> {
        Rules.check(x.state.equals("DRAFT"), "STATE_INVALID");
        Rules.check(source.enabled && target.enabled, "SITE_DISABLED");
        worker(x.senderId, "handover", x.departmentId);
        worker(x.receiverId, "handover", x.departmentId);
        capacity(target, x.colonyIds.size());
        var frozen = new ArrayList<Object>();
        for (Long cid : x.colonyIds) {
          var c = colony(cid);
          active(c);
          noPending(c);
          Rules.check(c.apiaryId.equals(source.id), "SNAPSHOT_STALE");
          chronological(c, x.plannedOn);
          c.moveId = x.id;
          c.version++;
          frozen.add(identity(c));
        }
        x.snapshotJson =
            encode(
                Map.of(
                    "code",
                    x.code,
                    "sourceCode",
                    source.code,
                    "sourceName",
                    source.name,
                    "destinationCode",
                    target.code,
                    "destinationName",
                    target.name,
                    "senderId",
                    x.senderId,
                    "receiverId",
                    x.receiverId,
                    "plannedOn",
                    x.plannedOn.toString(),
                    "colonies",
                    frozen));
        x.digest = digest(x.snapshotJson);
        x.state = "RELEASED";
      }
      case "cancel" -> {
        Rules.check(Set.of("DRAFT", "RELEASED").contains(x.state), "STATE_INVALID");
        x.notes = note(b);
        if (x.state.equals("RELEASED")) {
          for (Long cid : x.colonyIds) {
            var c = db.get(Colony.class, cid);
            Rules.check(
                Objects.equals(c.moveId, x.id) && c.state.equals("ACTIVE"), "SNAPSHOT_STALE");
            c.moveId = null;
            c.version++;
          }
        }
        x.state = "CANCELLED";
      }
      case "depart" -> {
        Rules.check(x.state.equals("RELEASED"), "STATE_INVALID");
        hash(x.snapshotJson, x.digest);
        var day = date(b.get("happenedOn"), x.departmentId, false);
        Rules.check(!day.isBefore(x.plannedOn), "DATE_ORDER");
        var n = note(b);
        for (Long cid : x.colonyIds) {
          var c = db.get(Colony.class, cid);
          Rules.check(
              Objects.equals(c.moveId, x.id)
                  && c.state.equals("ACTIVE")
                  && c.apiaryId.equals(source.id),
              "SNAPSHOT_STALE");
          chronological(c, day);
          c.state = "IN_TRANSIT";
          c.lastOn = day;
          c.version++;
          event(
              c,
              "DEPART",
              day,
              n,
              Map.of(
                  "transferId", x.id, "digest", x.digest, "from", source.code, "to", target.code));
        }
        x.departedOn = day;
        x.state = "IN_TRANSIT";
      }
      case "receive" -> {
        Rules.check(x.state.equals("IN_TRANSIT"), "STATE_INVALID");
        hash(x.snapshotJson, x.digest);
        var day = date(b.get("happenedOn"), x.departmentId, false);
        Rules.check(!day.isBefore(x.departedOn), "DATE_ORDER");
        Rules.check(target.enabled, "SITE_DISABLED");
        capacity(target, 0);
        var n = note(b);
        for (Long cid : x.colonyIds) {
          var c = db.get(Colony.class, cid);
          Rules.check(
              Objects.equals(c.moveId, x.id)
                  && c.state.equals("IN_TRANSIT")
                  && c.apiaryId.equals(source.id),
              "SNAPSHOT_STALE");
          c.apiaryId = target.id;
          c.state = "ACTIVE";
          c.moveId = null;
          c.lastOn = day;
          c.version++;
          event(
              c,
              "RECEIVE",
              day,
              n,
              Map.of(
                  "transferId", x.id, "digest", x.digest, "from", source.code, "to", target.code));
        }
        x.receivedOn = day;
        x.state = "RECEIVED";
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    x.version++;
    access.audit("TRANSFER_" + action.toUpperCase(Locale.ROOT), x.id, x.departmentId);
    return x;
  }

  /** 详情按岗位范围返回，包含独立事件、历史观察和分群关系。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object detail(String kind, Long id) {
    return switch (kind) {
      case "colonies" -> {
        access.require("colonies");
        var c = colony(id);
        var list =
            db.query(Inspection.class, "from Inspection where colonyId=?1", id).stream()
                .filter(this::inspectionVisible)
                .map(this::inspectionView)
                .toList();
        yield Map.of(
            "colony",
            c,
            "inspections",
            list,
            "events",
            db.query(ColonyEvent.class, "from ColonyEvent where colonyId=?1 order by id desc", id),
            "children",
            db.query(Colony.class, "from Colony where parentId=?1", id).stream()
                .filter(this::colonyVisible)
                .toList());
      }
      case "inspections" -> {
        access.require("colonies");
        yield inspectionView(inspection(id));
      }
      case "transfers" -> {
        access.require("transfers");
        var x = transfer(id);
        if (!x.snapshotJson.isEmpty()) hash(x.snapshotJson, x.digest);
        yield x;
      }
      default -> throw new Problem(404, "NOT_FOUND");
    };
  }

  Map<String, Object> inspectionView(Inspection x) {
    if (!x.snapshotJson.isEmpty()) hash(x.snapshotJson, x.digest);
    return Map.of("record", x, "superseded", superseded(x.id));
  }

  /** 授权统计仅计已核实且未被代替的记录，无自动诊断或绩效结论。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object report() {
    access.require("reports");
    var cs = db.all(Colony.class).stream().filter(this::colonyVisible).toList();
    var ins = db.all(Inspection.class).stream().filter(this::inspectionVisible).toList();
    var moves = db.all(Transfer.class).stream().filter(this::transferVisible).toList();
    Map<String, Long> states = new LinkedHashMap<>();
    for (String s : List.of("ACTIVE", "IN_TRANSIT", "RETIRED"))
      states.put(s, cs.stream().filter(c -> c.state.equals(s)).count());
    var siteRows = new ArrayList<Object>();
    for (var s : db.all(ApiarySite.class)) {
      var own =
          cs.stream().filter(c -> c.apiaryId.equals(s.id) && !c.state.equals("RETIRED")).toList();
      if (access.department(s.departmentId) && (!assigned() || !own.isEmpty()))
        siteRows.add(
            Map.of("code", s.code, "name", s.name, "colonies", own.size(), "capacity", s.capacity));
    }
    return Map.of(
        "colonies",
        cs.size(),
        "byState",
        states,
        "pending",
        ins.stream().filter(x -> x.state.equals("PENDING")).count(),
        "verified",
        ins.stream().filter(x -> x.state.equals("VERIFIED") && !superseded(x.id)).count(),
        "received",
        moves.stream().filter(x -> x.state.equals("RECEIVED")).count(),
        "inTransit",
        moves.stream().filter(x -> x.state.equals("IN_TRANSIT")).count(),
        "sites",
        siteRows);
  }

  /** UTF-8 CSV沿用读取和导出范围，并中和公式前缀；快照不用当前位置覆盖。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public String export(String kind, Long id) {
    access.require("export");
    if (kind.equals("inspections")) {
      access.require("colonies");
      var x = inspection(id);
      Rules.check(x.state.equals("VERIFIED"), "STATE_INVALID");
      hash(x.snapshotJson, x.digest);
      var p = decodeSnapshot(x.snapshotJson);
      StringBuilder csv =
          new StringBuilder(
              "\uFEFFinspection_id,colony_code,colony_name,apiary_code,apiary_name,observed_on,covered_frames,brood_frames,queen_seen,stores,brood_pattern,observations,inspector_id,reviewer_id,supersedes_id,sha256\r\n");
      csv.append(
          csvRow(
              x.id,
              p.get("code"),
              p.get("name"),
              p.get("apiaryCode"),
              p.get("apiaryName"),
              p.get("observedOn"),
              p.get("coveredFrames"),
              p.get("broodFrames"),
              p.get("queenSeen"),
              p.get("stores"),
              p.get("broodPattern"),
              p.get("observations"),
              p.get("inspectorId"),
              p.get("reviewerId"),
              p.get("supersedesId"),
              x.digest));
      return csv.toString();
    }
    if (kind.equals("transfers")) {
      access.require("transfers");
      var x = transfer(id);
      Rules.check(!x.snapshotJson.isEmpty(), "STATE_INVALID");
      hash(x.snapshotJson, x.digest);
      var p = decodeSnapshot(x.snapshotJson);
      StringBuilder csv =
          new StringBuilder(
              "\uFEFFtransfer_id,transfer_code,state,origin_code,destination_code,planned_on,departed_on,received_on,sender_id,receiver_id,colony_id,colony_code,colony_name,queen_code,sha256\r\n");
      if (!(p.get("colonies") instanceof List<?> list)) throw new Problem(409, "SNAPSHOT_CHANGED");
      for (var entry : list) {
        if (!(entry instanceof Map<?, ?> c)) throw new Problem(409, "SNAPSHOT_CHANGED");
        csv.append(
            csvRow(
                x.id,
                p.get("code"),
                x.state,
                p.get("sourceCode"),
                p.get("destinationCode"),
                p.get("plannedOn"),
                x.departedOn,
                x.receivedOn,
                p.get("senderId"),
                p.get("receiverId"),
                c.get("id"),
                c.get("code"),
                c.get("name"),
                c.get("queenCode"),
                x.digest));
      }
      return csv.toString();
    }
    throw new Problem(404, "NOT_FOUND");
  }

  private Map<String, Object> decodeSnapshot(String source) {
    try {
      return json.readValue(
          source, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new Problem(409, "SNAPSHOT_CHANGED");
    }
  }

  private String csvRow(Object... values) {
    return Arrays.stream(values).map(Rules::csv).collect(java.util.stream.Collectors.joining(","))
        + "\r\n";
  }
}
