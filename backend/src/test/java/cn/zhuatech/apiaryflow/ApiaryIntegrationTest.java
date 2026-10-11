// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.apiaryflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 真实HTTP/Security/JPA/Flyway测试，私有随机密码与TEST记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class ApiaryIntegrationTest {
  static final String PW = "Aa9" + UUID.randomUUID();
  final String day = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:apiaryflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    r.add("spring.datasource.username", () -> "sa");
    r.add("spring.datasource.password", () -> "");
    r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    r.add("apiaryflow.admin-password", () -> PW);
  }

  @Autowired MockMvc mvc;
  final JsonMapper json = JsonMapper.builder().build();
  MockHttpSession root, keeper, reviewer, receiver;
  long keeperId, reviewerId, receiverId, s1, s2, c1;
  String suffix;

  Map<String, Object> m(Object... v) {
    var b = new LinkedHashMap<String, Object>();
    for (int i = 0; i < v.length; i += 2) b.put(v[i].toString(), v[i + 1]);
    return b;
  }

  MockHttpServletRequestBuilder req(String p, String method, Map<String, Object> b) {
    var r =
        switch (method) {
          case "POST" -> post("/api" + p);
          case "PUT" -> put("/api" + p);
          case "DELETE" -> delete("/api" + p);
          default -> get("/api" + p);
        };
    if (b != null)
      r.with(csrf()).contentType("application/json").content(json.writeValueAsString(b));
    return r;
  }

  MvcResult result(MockHttpSession s, String p, String method, Map<String, Object> b)
      throws Exception {
    return mvc.perform(req(p, method, b).session(s)).andReturn();
  }

  JsonNode call(MockHttpSession s, String p, String method, Map<String, Object> b)
      throws Exception {
    var r = result(s, p, method, b);
    assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    return json.readTree(r.getResponse().getContentAsString());
  }

  void fail(
      MockHttpSession s, String p, String method, Map<String, Object> b, int status, String code)
      throws Exception {
    var r = result(s, p, method, b);
    assertEquals(status, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    assertEquals(code, json.readTree(r.getResponse().getContentAsString()).get("code").asString());
  }

  MockHttpSession login(String u) throws Exception {
    var r = mvc.perform(req("/auth/login", "POST", m("username", u, "password", PW))).andReturn();
    assertEquals(200, r.getResponse().getStatus());
    return (MockHttpSession) r.getRequest().getSession();
  }

  long user(String u, long role, long dept) throws Exception {
    return call(
            root,
            "/admin/users",
            "POST",
            m(
                "username",
                u,
                "displayName",
                "TEST " + u,
                "password",
                PW,
                "roleId",
                role,
                "departmentId",
                dept,
                "enabled",
                true))
        .get("id")
        .asLong();
  }

  long site(String code, int capacity, long dept) throws Exception {
    return call(
            root,
            "/apiaries",
            "POST",
            m(
                "code",
                code,
                "name",
                "TEST " + code,
                "departmentId",
                dept,
                "location",
                "TEST site",
                "capacity",
                capacity,
                "enabled",
                true))
        .get("id")
        .asLong();
  }

  Map<String, Object> colonyBody(String code, long site, long user) {
    return m(
        "code",
        code,
        "name",
        "TEST " + code,
        "apiaryId",
        site,
        "keeperId",
        user,
        "category",
        "MELLIFERA",
        "source",
        "TEST local register",
        "openedOn",
        day);
  }

  JsonNode colony() throws Exception {
    return call(root, "/colonies/" + c1, "GET", null).get("colony");
  }

  Map<String, Object> inspectionBody() {
    return m(
        "colonyId",
        c1,
        "observedOn",
        day,
        "coveredFrames",
        8,
        "broodFrames",
        5,
        "queenSeen",
        true,
        "stores",
        "NORMAL",
        "broodPattern",
        "EVEN",
        "observations",
        "TEST observation\nNo treatment advice",
        "reviewerId",
        reviewerId);
  }

  JsonNode inspection() throws Exception {
    return call(keeper, "/inspections", "POST", inspectionBody());
  }

  JsonNode submit(JsonNode x) throws Exception {
    return call(
        keeper,
        "/inspections/" + x.get("id").asLong() + "/submit",
        "POST",
        m("version", x.get("version").asLong()));
  }

  JsonNode verified() throws Exception {
    var x = submit(inspection());
    return call(
        reviewer,
        "/inspections/" + x.get("id").asLong() + "/verify",
        "POST",
        m("version", x.get("version").asLong(), "note", "TEST independently checked"));
  }

  Map<String, Object> transferBody() {
    return m(
        "code",
        "MOVE_" + suffix,
        "sourceId",
        s1,
        "destinationId",
        s2,
        "senderId",
        keeperId,
        "receiverId",
        receiverId,
        "plannedOn",
        day,
        "notes",
        "TEST relocation",
        "colonyIds",
        List.of(c1));
  }

  JsonNode transfer() throws Exception {
    return call(root, "/transfers", "POST", transferBody());
  }

  JsonNode release() throws Exception {
    var x = transfer();
    return call(
        root,
        "/transfers/" + x.get("id").asLong() + "/release",
        "POST",
        m("version", x.get("version").asLong()));
  }

  JsonNode depart(JsonNode x) throws Exception {
    return call(
        keeper,
        "/transfers/" + x.get("id").asLong() + "/depart",
        "POST",
        m(
            "version",
            x.get("version").asLong(),
            "happenedOn",
            day,
            "note",
            "TEST physical departure"));
  }

  @BeforeEach
  void setup() throws Exception {
    suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    root = login("admin");
    keeperId = user("k" + suffix, 3, 1);
    reviewerId = user("v" + suffix, 4, 1);
    receiverId = user("r" + suffix, 3, 1);
    keeper = login("k" + suffix);
    reviewer = login("v" + suffix);
    receiver = login("r" + suffix);
    s1 = site("SRC_" + suffix, 20, 1);
    s2 = site("DST_" + suffix, 20, 1);
    c1 =
        call(root, "/colonies", "POST", colonyBody("COL_" + suffix, s1, keeperId))
            .get("id")
            .asLong();
  }

  @Test
  void fullInspectionAndReceiptPreserveFrozenApiary() throws Exception {
    var v = verified();
    String snap = v.get("snapshotJson").asString(), hash = v.get("digest").asString();
    var x = depart(release());
    var done =
        call(
            receiver,
            "/transfers/" + x.get("id").asLong() + "/receive",
            "POST",
            m(
                "version",
                x.get("version").asLong(),
                "happenedOn",
                day,
                "note",
                "TEST counted and received"));
    assertEquals("RECEIVED", done.get("state").asString());
    assertEquals(s2, colony().get("apiaryId").asLong());
    var old = call(keeper, "/inspections/" + v.get("id").asLong(), "GET", null).get("record");
    assertEquals(snap, old.get("snapshotJson").asString());
    assertEquals(hash, old.get("digest").asString());
  }

  @Test
  void correctionAppendsAndPreservesOriginal() throws Exception {
    var old = verified();
    var b = inspectionBody();
    b.put("supersedesId", old.get("id").asLong());
    b.put("broodFrames", 4);
    var next = submit(call(keeper, "/inspections", "POST", b));
    call(
        reviewer,
        "/inspections/" + next.get("id").asLong() + "/verify",
        "POST",
        m("version", next.get("version").asLong(), "note", "TEST correction checked"));
    var before = call(keeper, "/inspections/" + old.get("id").asLong(), "GET", null);
    assertTrue(before.get("superseded").asBoolean());
    assertEquals(5, before.get("record").get("broodFrames").asInt());
    assertEquals(old.get("digest").asString(), before.get("record").get("digest").asString());
    fail(keeper, "/inspections", "POST", b, 409, "REVISION_INVALID");
  }

  @Test
  void reviewerCanReturnThenKeeperResubmits() throws Exception {
    var x = submit(inspection());
    x =
        call(
            reviewer,
            "/inspections/" + x.get("id").asLong() + "/return",
            "POST",
            m("version", x.get("version").asLong(), "note", "TEST clarify observations"));
    assertEquals("DRAFT", x.get("state").asString());
    var b = inspectionBody();
    b.put("version", x.get("version").asLong());
    b.put("observations", "TEST clarified facts");
    x = call(keeper, "/inspections/" + x.get("id").asLong(), "PUT", b);
    assertEquals("PENDING", submit(x).get("state").asString());
  }

  @Test
  void verifiedRecordCannotBeEditedOrDeleted() throws Exception {
    var x = verified();
    var b = inspectionBody();
    b.put("version", x.get("version").asLong());
    fail(keeper, "/inspections/" + x.get("id").asLong(), "PUT", b, 409, "STATE_INVALID");
    fail(keeper, "/inspections/" + x.get("id").asLong(), "DELETE", b, 409, "REFERENCED");
  }

  @Test
  void adminCannotReplaceAssignedReviewer() throws Exception {
    var x = submit(inspection());
    fail(
        root,
        "/inspections/" + x.get("id").asLong() + "/verify",
        "POST",
        m("version", x.get("version").asLong(), "note", "TEST wrong person"),
        409,
        "REVIEWER_ONLY");
  }

  @Test
  void inspectorCannotReviewOwnFactsEvenWithAllPermissions() throws Exception {
    long rootId = call(root, "/auth/me", "GET", null).get("id").asLong();
    var own = call(root, "/colonies", "POST", colonyBody("SELF_" + suffix, s1, rootId));
    var b = inspectionBody();
    b.put("colonyId", own.get("id").asLong());
    b.put("reviewerId", rootId);
    fail(root, "/inspections", "POST", b, 409, "INDEPENDENT_REQUIRED");
  }

  @Test
  void otherKeeperCannotSeeOrInspectColony() throws Exception {
    fail(receiver, "/colonies/" + c1, "GET", null, 403, "OUT_OF_SCOPE");
    fail(receiver, "/inspections", "POST", inspectionBody(), 403, "OUT_OF_SCOPE");
  }

  @Test
  void releaseReservesColonyAndCannotDoubleBook() throws Exception {
    release();
    fail(keeper, "/inspections", "POST", inspectionBody(), 409, "COLONY_BUSY");
    var b = transferBody();
    b.put("code", "OTHER_" + suffix);
    fail(root, "/transfers", "POST", b, 409, "COLONY_BUSY");
  }

  @Test
  void pendingInspectionPreventsRelease() throws Exception {
    submit(inspection());
    var x = transfer();
    fail(
        root,
        "/transfers/" + x.get("id").asLong() + "/release",
        "POST",
        m("version", x.get("version").asLong()),
        409,
        "INSPECTION_PENDING");
  }

  @Test
  void cancelledReleaseReopensColony() throws Exception {
    var x = release();
    x =
        call(
            root,
            "/transfers/" + x.get("id").asLong() + "/cancel",
            "POST",
            m("version", x.get("version").asLong(), "note", "TEST cancelled before departure"));
    assertEquals("CANCELLED", x.get("state").asString());
    assertTrue(colony().get("moveId").isNull());
    assertEquals("DRAFT", inspection().get("state").asString());
  }

  @Test
  void inTransitCannotCancelOrChangeQueen() throws Exception {
    var x = depart(release());
    fail(
        root,
        "/transfers/" + x.get("id").asLong() + "/cancel",
        "POST",
        m("version", x.get("version").asLong(), "note", "TEST invalid cancellation"),
        409,
        "STATE_INVALID");
    fail(
        root,
        "/colonies/" + c1 + "/requeen",
        "POST",
        m(
            "version",
            colony().get("version").asLong(),
            "happenedOn",
            day,
            "note",
            "TEST change queen",
            "queenCode",
            "Q1"),
        409,
        "COLONY_BUSY");
  }

  @Test
  void wrongOperatorAndPrematureReceiveRejected() throws Exception {
    var x = release();
    fail(
        receiver,
        "/transfers/" + x.get("id").asLong() + "/depart",
        "POST",
        m("version", x.get("version").asLong(), "happenedOn", day, "note", "TEST wrong person"),
        409,
        "OPERATOR_ONLY");
    fail(
        receiver,
        "/transfers/" + x.get("id").asLong() + "/receive",
        "POST",
        m("version", x.get("version").asLong(), "happenedOn", day, "note", "TEST too early"),
        409,
        "STATE_INVALID");
  }

  @Test
  void capacityIncludesPublishedReservations() throws Exception {
    var c2 = call(root, "/colonies", "POST", colonyBody("TWO_" + suffix, s1, keeperId));
    var target = call(root, "/apiaries", "GET", null);
    JsonNode s = null;
    for (var row : target) if (row.get("id").asLong() == s2) s = row;
    var b = json.convertValue(s, Map.class);
    b.put("capacity", 1);
    call(root, "/apiaries/" + s2, "PUT", b);
    release();
    var tb = transferBody();
    tb.put("code", "SECOND_" + suffix);
    tb.put("colonyIds", List.of(c2.get("id").asLong()));
    var x = call(root, "/transfers", "POST", tb);
    fail(
        root,
        "/transfers/" + x.get("id").asLong() + "/release",
        "POST",
        m("version", x.get("version").asLong()),
        409,
        "CAPACITY_EXCEEDED");
  }

  @Test
  void splitCreatesLinkedIdentityWithoutRetiringParent() throws Exception {
    var c = colony();
    call(
        root,
        "/colonies/" + c1 + "/split",
        "POST",
        m(
            "version",
            c.get("version").asLong(),
            "happenedOn",
            day,
            "code",
            "CHILD_" + suffix,
            "name",
            "TEST daughter colony",
            "note",
            "TEST observed split"));
    var detail = call(root, "/colonies/" + c1, "GET", null);
    assertEquals("ACTIVE", detail.get("colony").get("state").asString());
    assertEquals(c1, detail.get("children").get(0).get("parentId").asLong());
    assertTrue(detail.get("events").size() >= 2);
  }

  @Test
  void queenChangesPreserveOldNewEvents() throws Exception {
    var c = colony();
    for (String q : List.of("TEST_Q1", "TEST_Q2"))
      c =
          call(
              root,
              "/colonies/" + c1 + "/requeen",
              "POST",
              m(
                  "version",
                  c.get("version").asLong(),
                  "happenedOn",
                  day,
                  "queenCode",
                  q,
                  "note",
                  "TEST documented queen change"));
    var detail = call(root, "/colonies/" + c1, "GET", null);
    assertEquals("TEST_Q2", detail.get("colony").get("queenCode").asString());
    assertTrue(detail.get("events").get(0).get("snapshotJson").asString().contains("TEST_Q1"));
  }

  @Test
  void retiredColonyKeepsHistoryAndCannotInspect() throws Exception {
    verified();
    var c = colony();
    call(
        root,
        "/colonies/" + c1 + "/retire",
        "POST",
        m("version", c.get("version").asLong(), "happenedOn", day, "note", "TEST closed colony"));
    fail(keeper, "/inspections", "POST", inspectionBody(), 409, "COLONY_BUSY");
    assertEquals(1, call(root, "/colonies/" + c1, "GET", null).get("inspections").size());
  }

  @Test
  void excessiveFramesAndFutureObservationsRejected() throws Exception {
    var b = inspectionBody();
    b.put("broodFrames", 9);
    fail(keeper, "/inspections", "POST", b, 409, "FRAME_INVALID");
    b = inspectionBody();
    b.put("observedOn", LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString());
    fail(keeper, "/inspections", "POST", b, 409, "DATE_INVALID");
  }

  @Test
  void optimisticVersionRejectsStaleUpdates() throws Exception {
    var c = colony();
    var b = colonyBody(c.get("code").asString(), s1, keeperId);
    b.put("version", c.get("version").asLong());
    b.put("name", "TEST changed");
    call(root, "/colonies/" + c1, "PUT", b);
    b.put("name", "TEST stale overwrite");
    fail(root, "/colonies/" + c1, "PUT", b, 409, "VERSION_CONFLICT");
    assertEquals("TEST changed", colony().get("name").asString());
  }

  @Test
  void csrfAndAnonymousAccessProtected() throws Exception {
    assertEquals(401, mvc.perform(get("/api/colonies")).andReturn().getResponse().getStatus());
    assertEquals(
        403,
        mvc.perform(
                post("/api/colonies").session(root).contentType("application/json").content("{}"))
            .andReturn()
            .getResponse()
            .getStatus());
    fail(keeper, "/admin/users", "GET", null, 403, "FORBIDDEN");
  }

  @Test
  void disabledAccountRevokesExistingSession() throws Exception {
    var users = call(root, "/admin/users", "GET", null);
    JsonNode u = null;
    for (var row : users) if (row.get("id").asLong() == keeperId) u = row;
    var b = json.convertValue(u, Map.class);
    b.put("enabled", false);
    call(root, "/admin/users/" + keeperId, "PUT", b);
    fail(keeper, "/auth/me", "GET", null, 401, "UNAUTHENTICATED");
  }

  @Test
  void crossDepartmentDeniedEvenWithOwnAssignedRole() throws Exception {
    var d =
        call(
            root,
            "/admin/departments",
            "POST",
            m("name", "TEST other " + suffix, "zone", "UTC", "enabled", true));
    long did = d.get("id").asLong(),
        kid = user("x" + suffix, 3, did),
        sid = site("CROSS_" + suffix, 5, did);
    long other =
        call(root, "/colonies", "POST", colonyBody("REMOTE_" + suffix, sid, kid))
            .get("id")
            .asLong();
    fail(keeper, "/colonies/" + other, "GET", null, 403, "OUT_OF_SCOPE");
  }

  @Test
  void exportMatchesFrozenRecordAndRequiresExportPermission() throws Exception {
    var x = verified();
    var res = result(keeper, "/inspections/" + x.get("id").asLong() + "/export.csv", "GET", null);
    assertEquals(200, res.getResponse().getStatus());
    assertTrue(res.getResponse().getContentAsString().contains(x.get("digest").asString()));
    assertFalse(res.getResponse().getContentAsString().contains("password"));
    assertEquals(
        200,
        mvc.perform(
                head("/api/inspections/" + x.get("id").asLong() + "/export.csv").session(keeper))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void senderReceiverMustDifferAndManifestCannotDuplicate() throws Exception {
    var b = transferBody();
    b.put("receiverId", keeperId);
    fail(root, "/transfers", "POST", b, 409, "INDEPENDENT_REQUIRED");
    b = transferBody();
    b.put("colonyIds", List.of(c1, c1));
    fail(root, "/transfers", "POST", b, 409, "DUPLICATE_COLONY");
  }

  @Test
  void lastAdministratorCannotBeDisabled() throws Exception {
    var us = call(root, "/admin/users", "GET", null);
    JsonNode a = null;
    for (var row : us) if (row.get("username").asString().equals("admin")) a = row;
    var b = json.convertValue(a, Map.class);
    b.put("enabled", false);
    fail(root, "/admin/users/" + a.get("id").asLong(), "PUT", b, 409, "LAST_ADMIN");
    assertEquals("admin", call(root, "/auth/me", "GET", null).get("username").asString());
  }

  @Test
  void correctionAfterTransferKeepsOriginalLocationContext() throws Exception {
    var original = verified();
    var moved = depart(release());
    call(
        receiver,
        "/transfers/" + moved.get("id").asLong() + "/receive",
        "POST",
        m(
            "version",
            moved.get("version").asLong(),
            "happenedOn",
            day,
            "note",
            "TEST receipt before correction"));
    var body = inspectionBody();
    body.put("supersedesId", original.get("id").asLong());
    body.put("observations", "TEST corrected original inspection");
    var correction = submit(call(keeper, "/inspections", "POST", body));
    var snap = json.readTree(correction.get("snapshotJson").asString());
    assertEquals(s1, snap.get("apiaryId").asLong());
    assertEquals(s2, colony().get("apiaryId").asLong());
    assertEquals(original.get("observedOn").asString(), snap.get("observedOn").asString());
  }
}
