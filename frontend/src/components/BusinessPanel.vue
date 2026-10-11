<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { Plus, Search, Pencil, ArrowLeft, X, Download } from "@lucide/vue";
import { api, download } from "../api.js";
import { t, stateName, cloneRecord, time } from "../ui.js";
const props = defineProps({
  section: { type: String, required: true },
  me: { type: Object, required: true },
  pending: Boolean,
  perform: { type: Function, required: true },
});
const emit = defineEmits(["error"]);
const rows = ref([]),
  colonies = ref([]),
  options = ref({ apiaries: [], people: [], categories: [] }),
  ready = ref(false),
  q = ref(""),
  state = ref(""),
  sort = ref("newest"),
  page = ref(1),
  editing = ref(null),
  detail = ref(null),
  acting = ref(null);
const has = (code) => props.me.permissions.includes(code);
const title = computed(
  () =>
    ({
      colonies: t("蜂群台账", "Colony register"),
      inspections: t("巡检与独立复核", "Inspections & review"),
      transfers: t("转场交接", "Apiary transfers"),
      apiaries: t("蜂场目录", "Apiary directory"),
    })[props.section],
);
const intro = computed(
  () =>
    ({
      colonies: t(
        "稳定身份、指定管护、分群关系与换王历史",
        "Stable identity, assigned keeper, split lineage and queen history",
      ),
      inspections: t(
        "观察事实 · 独立复核 · 追加修订",
        "Observed facts · Independent review · Append corrections",
      ),
      transfers: t(
        "冻结清单，分别确认移出与接收",
        "Freeze the manifest; confirm departure and receipt separately",
      ),
      apiaries: t(
        "场地身份、团队、位置与容量",
        "Site identity, team, location and capacity",
      ),
    })[props.section],
);
const canAdd = computed(() =>
  props.section === "inspections"
    ? has("inspect")
    : props.section === "transfers"
      ? has("plan")
      : has("manage") && props.me.scope !== "ASSIGNED",
);
const filtered = computed(() =>
  rows.value
    .filter(
      (r) =>
        (!state.value ||
          (r.state || (r.enabled ? "ENABLED" : "DISABLED")) === state.value) &&
        JSON.stringify(r).toLowerCase().includes(q.value.toLowerCase()),
    )
    .sort((a, b) =>
      sort.value === "name"
        ? String(a.name || a.code || a.id).localeCompare(
            String(b.name || b.code || b.id),
          )
        : sort.value === "oldest"
          ? a.id - b.id
          : b.id - a.id,
    ),
);
const visible = computed(() =>
  filtered.value.slice((page.value - 1) * 15, page.value * 15),
);
const statuses = computed(() => [
  ...new Set(
    rows.value.map((r) => r.state || (r.enabled ? "ENABLED" : "DISABLED")),
  ),
]);
const labelState = (s) =>
  s === "ENABLED"
    ? t("启用", "Enabled")
    : s === "DISABLED"
      ? t("停用", "Disabled")
      : stateName(s);
const siteName = (id) =>
  options.value.apiaries.find((x) => x.id === Number(id))?.name || "#" + id;
const personName = (id) =>
  options.value.people.find((x) => x.id === Number(id))?.displayName ||
  "#" + id;
const colonyName = (id) =>
  colonies.value.find((x) => x.id === Number(id))?.code || "#" + id;
const day = () =>
  new Intl.DateTimeFormat("en-CA", {
    timeZone: props.me.zone || "UTC",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
watch([q, state, sort], () => (page.value = 1));
watch(
  () => editing.value?.sourceId,
  (next, prev) => {
    if (
      props.section === "transfers" &&
      next !== prev &&
      prev != null &&
      editing.value
    )
      editing.value.colonyIds = [];
  },
);
/** 读取真实列表和最小候选目录，失败不显示虚假数据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function load() {
  ready.value = false;
  try {
    rows.value = await api("/" + props.section);
    options.value = await api("/options");
    if (has("colonies"))
      colonies.value =
        props.section === "colonies" ? rows.value : await api("/colonies");
  } catch (e) {
    emit("error", e);
  } finally {
    ready.value = true;
  }
}
/** 默认值仅准备表单，业务事实由用户填写和服务端校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function edit(row = null, override = {}) {
  if (row) {
    editing.value = cloneRecord(row);
    return;
  }
  const s = options.value.apiaries.find(
    (s) => s.enabled && s.departmentId === props.me.departmentId,
  );
  const c = colonies.value.find(
    (c) => c.keeperId === props.me.id && c.state === "ACTIVE" && !c.moveId,
  );
  editing.value = {
    ...(props.section === "apiaries"
      ? {
          code: "",
          name: "",
          departmentId: props.me.departmentId,
          location: "",
          capacity: 50,
          enabled: true,
        }
      : props.section === "colonies"
        ? {
            code: "",
            name: "",
            apiaryId: s?.id || "",
            keeperId: "",
            category: "UNKNOWN",
            source: "",
            openedOn: day(),
          }
        : props.section === "inspections"
          ? {
              colonyId: c?.id || "",
              observedOn: day(),
              coveredFrames: "",
              broodFrames: "",
              queenSeen: false,
              stores: "UNKNOWN",
              broodPattern: "UNKNOWN",
              observations: "",
              reviewerId: "",
            }
          : {
              code: "",
              sourceId: s?.id || "",
              destinationId: "",
              senderId: "",
              receiverId: "",
              plannedOn: day(),
              notes: "",
              colonyIds: [],
            }),
    ...override,
  };
}
const field = (key, zh, en, type = "text", extra = {}) => ({
  key,
  label: t(zh, en),
  type,
  ...extra,
});
const fields = computed(() => {
  const id = !!editing.value?.id;
  return props.section === "apiaries"
    ? [
        field("code", "稳定编号", "Stable code", "text", {
          readonly: id,
          maxlength: 30,
        }),
        field("name", "蜂场名称", "Apiary name"),
        field("departmentId", "团队标识", "Team ID", "number", {
          readonly: true,
        }),
        field("location", "位置描述", "Location", "text", { maxlength: 300 }),
        field(
          "capacity",
          "最大在场与预留数量",
          "Resident & reserved capacity",
          "number",
          { min: 1, max: 1000 },
        ),
        field("enabled", "启用", "Enabled", "checkbox"),
      ]
    : props.section === "colonies"
      ? [
          field("code", "蜂群编号", "Colony code", "text", {
            readonly: id,
            maxlength: 30,
          }),
          field("name", "蜂群名称", "Colony name"),
          field("apiaryId", "当前蜂场", "Current apiary", "site", {
            readonly: id,
          }),
          field("keeperId", "指定管护人", "Assigned keeper", "person", {
            permission: "inspect",
          }),
          field("category", "蜂群分类", "Species category", "category"),
          field("source", "来源说明", "Source", "text", {
            readonly: id,
            maxlength: 300,
          }),
          field("openedOn", "建档日期", "Registration date", "date", {
            readonly: id,
          }),
        ]
      : props.section === "inspections"
        ? [
            field("colonyId", "蜂群", "Colony", "colony", { readonly: id }),
            field("observedOn", "观察日期", "Observation date", "date", {
              readonly: !!editing.value?.supersedesId,
            }),
            field("coveredFrames", "蜂覆盖框数", "Frames covered", "number", {
              min: 0,
              max: 200,
            }),
            field("broodFrames", "子脾框数", "Brood frames", "number", {
              min: 0,
              max: 200,
            }),
            field("queenSeen", "本次见到蜂王", "Queen seen", "checkbox"),
            field("stores", "贮蜜观察", "Stores", "choice", {
              choices: [
                ["UNKNOWN", t("未确定", "Unknown")],
                ["LOW", t("偏少", "Low")],
                ["NORMAL", t("一般", "Normal")],
                ["HIGH", t("较多", "High")],
              ],
            }),
            field("broodPattern", "子脾观察", "Brood pattern", "choice", {
              choices: [
                ["UNKNOWN", t("未确定", "Unknown")],
                ["EVEN", t("均匀", "Even")],
                ["PATCHY", t("不均匀", "Patchy")],
                ["NONE", t("未见", "None observed")],
              ],
            }),
            field("observations", "观察说明", "Observations", "textarea", {
              maxlength: 2000,
            }),
            field(
              "reviewerId",
              "独立复核人",
              "Independent reviewer",
              "person",
              { permission: "review" },
            ),
          ]
        : [
            field("code", "交接编号", "Transfer code", "text", {
              readonly: id,
              maxlength: 30,
            }),
            field("sourceId", "移出蜂场", "Origin", "site"),
            field("destinationId", "接收蜂场", "Destination", "site"),
            field("senderId", "指定移出人", "Assigned sender", "person", {
              permission: "handover",
            }),
            field("receiverId", "指定接收人", "Assigned receiver", "person", {
              permission: "handover",
            }),
            field("plannedOn", "计划日期", "Planned date", "date"),
            field("notes", "转场说明", "Transfer notes", "textarea", {
              maxlength: 500,
            }),
            field("colonyIds", "蜂群清单", "Manifest", "manifest"),
          ];
});
function candidates(f) {
  const b = editing.value;
  const dept =
    props.section === "transfers"
      ? options.value.apiaries.find((s) => s.id === Number(b.sourceId))
          ?.departmentId
      : props.section === "inspections"
        ? colonies.value.find((c) => c.id === Number(b.colonyId))?.departmentId
        : options.value.apiaries.find((s) => s.id === Number(b.apiaryId))
            ?.departmentId;
  return options.value.people.filter(
    (p) =>
      p.departmentId === dept &&
      p.permissions.includes(f.permission) &&
      (f.key !== "reviewerId" || p.id !== props.me.id),
  );
}
const eligible = computed(() =>
  colonies.value.filter(
    (c) =>
      c.apiaryId === Number(editing.value?.sourceId) &&
      c.state === "ACTIVE" &&
      !c.moveId,
  ),
);
function canEdit(r) {
  return props.section === "apiaries"
    ? canAdd.value
    : props.section === "colonies"
      ? canAdd.value && r.state === "ACTIVE" && !r.moveId
      : props.section === "inspections"
        ? has("inspect") && r.inspectorId === props.me.id && r.state === "DRAFT"
        : has("plan") && r.state === "DRAFT";
}
/** 成功后重读列表与详情，错误保留表单。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function save() {
  const b = editing.value;
  const out = await props.perform(
    "/" + props.section + (b.id ? "/" + b.id : ""),
    b.id ? "PUT" : "POST",
    b,
  );
  if (out) {
    editing.value = null;
    await load();
    if (detail.value) await openDetail(out);
  }
}
/** 详情来自当前服务端，不在浏览器补造已核实事实。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function openDetail(r) {
  try {
    detail.value = await api("/" + props.section + "/" + r.id);
  } catch (e) {
    emit("error", e);
  }
}
/** 从持久化事件摘要展示旧新蜂王和分群编号，不生成谱系推断。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function eventSummary(event) {
  try {
    const p = JSON.parse(event.snapshotJson);
    return event.action === "REQUEEN"
      ? (p.oldQueen || "—") + " → " + p.newQueen
      : event.action === "SPLIT_TO"
        ? p.child?.code || ""
        : event.action === "SPLIT_FROM"
          ? p.code
          : event.action === "DEPART" || event.action === "RECEIVE"
            ? p.from + " → " + p.to
            : "";
  } catch {
    return "";
  }
}
const current = computed(() =>
  props.section === "colonies"
    ? detail.value?.colony
    : props.section === "inspections"
      ? detail.value?.record
      : detail.value,
);
const snapshot = computed(() => {
  try {
    return JSON.parse(current.value?.snapshotJson || "{}");
  } catch {
    return {};
  }
});
const actions = computed(() => {
  const r = current.value;
  if (!r) return [];
  if (props.section === "colonies")
    return has("manage") &&
      props.me.scope !== "ASSIGNED" &&
      r.state === "ACTIVE" &&
      !r.moveId
      ? ["split", "requeen", "retire"]
      : [];
  if (props.section === "inspections") {
    const out = [];
    if (r.inspectorId === props.me.id && has("inspect")) {
      if (r.state === "DRAFT") out.push("submit", "cancel");
      if (r.state === "PENDING") out.push("withdraw");
    }
    if (r.reviewerId === props.me.id && has("review") && r.state === "PENDING")
      out.push("verify", "return");
    return out;
  }
  if (props.section === "transfers") {
    const out = [];
    if (has("plan")) {
      if (r.state === "DRAFT") out.push("release", "cancel");
      if (r.state === "RELEASED") out.push("cancel");
    }
    if (has("handover")) {
      if (r.state === "RELEASED" && r.senderId === props.me.id)
        out.push("depart");
      if (r.state === "IN_TRANSIT" && r.receiverId === props.me.id)
        out.push("receive");
    }
    return out;
  }
  return [];
});
const observationName = (value) =>
  ({
    LOW: t("偏少", "Low"),
    NORMAL: t("一般", "Normal"),
    HIGH: t("较多", "High"),
    UNKNOWN: t("未确定", "Unknown"),
    EVEN: t("均匀", "Even"),
    PATCHY: t("不均匀", "Patchy"),
    NONE: t("未见", "None observed"),
  })[value] || value;
const eventName = (value) =>
  ({
    REGISTER: t("建档", "Registered"),
    DETAILS_UPDATE: t("资料维护", "Details updated"),
    SPLIT_FROM: t("由父群分出", "Split from parent"),
    SPLIT_TO: t("登记分群", "Daughter registered"),
    REQUEEN: t("蜂王更换", "Queen changed"),
    RETIRE: t("退出登记", "Retired"),
    DEPART: t("转场移出", "Transfer departed"),
    RECEIVE: t("转场接收", "Transfer received"),
    INSPECTION_VERIFY: t("巡检核实", "Inspection verified"),
  })[value] || value;
const actionName = (a) =>
  ({
    split: t("登记分群", "Register split"),
    requeen: t("登记换王", "Record queen change"),
    retire: t("登记退出", "Retire colony"),
    submit: t("送独立复核", "Submit for review"),
    cancel: t("取消记录", "Cancel record"),
    withdraw: t("撤回修改", "Withdraw"),
    verify: t("核实观察", "Verify observations"),
    return: t("退回修改", "Return"),
    release: t("发布交接", "Release manifest"),
    depart: t("确认移出", "Confirm departure"),
    receive: t("确认接收", "Confirm receipt"),
  })[a] || a;
/** 办理前填写真实日期与说明，不在浏览器自动完成交接。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function act(action) {
  acting.value = {
    action,
    version: current.value.version,
    happenedOn: day(),
    note: "",
    code: "",
    name: "",
    queenCode: "",
  };
}
async function performAction() {
  const b = acting.value;
  const out = await props.perform(
    "/" + props.section + "/" + current.value.id + "/" + b.action,
    "POST",
    b,
  );
  if (out) {
    acting.value = null;
    await load();
    await openDetail(out);
  }
}
/** 修订保留原观察日期与旧记录引用，创建新草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function correct() {
  const r = current.value;
  const copy = cloneRecord(r);
  delete copy.id;
  delete copy.version;
  delete copy.snapshotJson;
  delete copy.digest;
  copy.supersedesId = r.id;
  copy.reviewerId = "";
  editing.value = copy;
}
async function exportRecord() {
  try {
    await download(
      "/" + props.section + "/" + current.value.id + "/export.csv",
      "apiaryflow-" + props.section + "-" + current.value.id + ".csv",
    );
  } catch (e) {
    emit("error", e);
  }
}
async function remove() {
  const r = current.value;
  if (
    !window.confirm(
      t(
        "仅删除从未使用的误建记录，确认删除？",
        "Delete this never-used erroneous record?",
      ),
    )
  )
    return;
  const out = await props.perform("/" + props.section + "/" + r.id, "DELETE", {
    version: r.version,
  });
  if (out) {
    detail.value = null;
    await load();
  }
}
onMounted(load);
</script>
<template>
  <section>
    <header class="page-heading">
      <div>
        <p class="eyebrow">{{ section.toUpperCase() }}</p>
        <h1>{{ title }}</h1>
        <p class="muted">{{ intro }}</p>
      </div>
      <div class="actions">
        <button :disabled="pending" @click="load">
          {{ t("刷新", "Refresh") }}</button
        ><button
          v-if="canAdd"
          class="primary"
          :disabled="pending || !ready"
          @click="edit()"
        >
          <Plus :size="16" />{{ t("新增", "Add") }}
        </button>
      </div>
    </header>
    <template v-if="detail && current">
      <button :disabled="pending" @click="detail = null">
        <ArrowLeft :size="16" />{{ t("返回列表", "Back to list") }}
      </button>
      <div class="panel">
        <header class="page-heading">
          <div>
            <h2>
              {{
                current.name ||
                current.code ||
                t("巡检记录", "Inspection") + " #" + current.id
              }}
            </h2>
            <span class="badge" :class="current.state">{{
              stateName(current.state)
            }}</span
            ><small class="muted">
              #{{ current.id }} · {{ t("版本", "Version") }}
              {{ current.version }}</small
            >
          </div>
          <div class="actions">
            <button
              v-if="canEdit(current)"
              :disabled="pending"
              @click="edit(current)"
            >
              <Pencil :size="16" />{{ t("编辑", "Edit") }}</button
            ><button
              v-if="
                has('export') &&
                ((section === 'inspections' && current.state === 'VERIFIED') ||
                  (section === 'transfers' && current.snapshotJson))
              "
              :disabled="pending"
              @click="exportRecord"
            >
              <Download :size="16" />CSV</button
            ><button
              v-if="
                (section === 'colonies' && canEdit(current)) ||
                (section === 'inspections' &&
                  canEdit(current) &&
                  !current.snapshotJson)
              "
              :disabled="pending"
              @click="remove"
            >
              {{ t("删除误建", "Delete unused") }}
            </button>
          </div>
        </header>
        <div class="record-actions">
          <button
            v-for="action in actions"
            :key="action"
            :class="
              ['verify', 'receive', 'release'].includes(action) ? 'primary' : ''
            "
            :disabled="pending"
            @click="act(action)"
          >
            {{ actionName(action) }}</button
          ><button
            v-if="
              section === 'inspections' &&
              current.state === 'VERIFIED' &&
              !detail.superseded &&
              current.inspectorId === me.id &&
              has('inspect')
            "
            :disabled="pending"
            @click="correct"
          >
            {{ t("追加修订", "Append correction") }}
          </button>
        </div>
        <template v-if="section === 'colonies'"
          ><div class="detail-grid">
            <div>
              <small>{{ t("稳定编号", "Stable code") }}</small
              ><strong>{{ current.code }}</strong>
            </div>
            <div>
              <small>{{ t("当前蜂场", "Current apiary") }}</small
              >{{ siteName(current.apiaryId) }}
            </div>
            <div>
              <small>{{ t("指定管护人", "Keeper") }}</small
              >{{ personName(current.keeperId) }}
            </div>
            <div>
              <small>{{
                t("建档 / 最新事实日期", "Registration / latest fact")
              }}</small
              >{{ current.openedOn }} / {{ current.lastOn }}
            </div>
            <div>
              <small>{{ t("蜂王编号", "Queen code") }}</small
              >{{ current.queenCode || "—" }} · {{ current.queenSince || "—" }}
            </div>
            <div>
              <small>{{ t("分群来源", "Split parent") }}</small
              >{{ current.parentId ? colonyName(current.parentId) : "—" }}
            </div>
          </div>
          <p class="record-note">{{ current.source }}</p>
          <p v-if="current.moveId" class="hint">
            {{ t("已预留转场交接", "Reserved transfer") }} #{{ current.moveId }}
          </p>
          <h3>{{ t("分出的蜂群", "Daughter colonies") }}</h3>
          <p v-if="!detail.children.length" class="muted">
            {{ t("暂无分群记录", "No split recorded") }}
          </p>
          <button
            v-for="c in detail.children"
            :key="c.id"
            :disabled="pending"
            @click="openDetail(c)"
          >
            {{ c.code }} · {{ c.name }}
          </button>
          <h3>{{ t("蜂群事件", "Colony timeline") }}</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ t("事实日期", "Fact date") }}</th>
                  <th>{{ t("事件", "Event") }}</th>
                  <th>{{ t("办理人", "Actor") }}</th>
                  <th>{{ t("说明", "Note") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="e in detail.events" :key="e.id">
                  <td>{{ e.happenedOn }}</td>
                  <td>{{ eventName(e.action) }}</td>
                  <td>{{ personName(e.actorId) }}</td>
                  <td class="record-note">
                    {{ e.notes }}
                    <small class="mono">{{ eventSummary(e) }}</small
                    ><small class="muted"> · {{ time(e.createdAt) }}</small>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <h3>{{ t("巡检历史", "Inspection history") }}</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ t("日期", "Date") }}</th>
                  <th>{{ t("状态", "State") }}</th>
                  <th>{{ t("框数", "Covered / brood") }}</th>
                  <th>{{ t("记录人 / 复核人", "Inspector / reviewer") }}</th>
                  <th>{{ t("修订", "Correction") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in detail.inspections" :key="item.record.id">
                  <td>{{ item.record.observedOn }}</td>
                  <td>{{ stateName(item.record.state) }}</td>
                  <td>
                    {{ item.record.coveredFrames }} /
                    {{ item.record.broodFrames }}
                  </td>
                  <td>
                    {{ personName(item.record.inspectorId) }} /
                    {{ personName(item.record.reviewerId) }}
                  </td>
                  <td>
                    {{
                      item.superseded
                        ? t("已被修订", "Superseded")
                        : item.record.supersedesId
                          ? "↩ #" + item.record.supersedesId
                          : "—"
                    }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div></template
        >
        <template v-else-if="section === 'inspections'"
          ><div class="detail-grid">
            <div>
              <small>{{ t("蜂群", "Colony") }}</small
              >{{ snapshot.code || colonyName(current.colonyId) }}
            </div>
            <div>
              <small>{{ t("观察日期", "Observed") }}</small
              >{{ current.observedOn }}
            </div>
            <div>
              <small>{{ t("现场蜂场", "Apiary at submission") }}</small
              >{{ snapshot.apiaryName || "—" }}
            </div>
            <div>
              <small>{{
                t("蜂覆盖 / 子脾框数", "Covered / brood frames")
              }}</small
              >{{ current.coveredFrames }} / {{ current.broodFrames }}
            </div>
            <div>
              <small>{{ t("是否见王", "Queen seen") }}</small
              >{{ current.queenSeen ? t("是", "Yes") : t("否", "No") }}
            </div>
            <div>
              <small>{{ t("贮蜜 / 子脾观察", "Stores / brood") }}</small
              >{{ observationName(current.stores) }} /
              {{ observationName(current.broodPattern) }}
            </div>
            <div>
              <small>{{ t("记录人", "Inspector") }}</small
              >{{ personName(current.inspectorId) }}
            </div>
            <div>
              <small>{{ t("独立复核人", "Reviewer") }}</small
              >{{ personName(current.reviewerId) }}
            </div>
            <div>
              <small>{{ t("修订关系", "Correction") }}</small
              >{{ current.supersedesId ? "↩ #" + current.supersedesId : "—" }}
              {{ detail.superseded ? t("已被修订", "Superseded") : "" }}
            </div>
          </div>
          <h3>{{ t("现场观察", "Observations") }}</h3>
          <p class="record-note">{{ current.observations }}</p>
          <h3>{{ t("复核说明", "Review note") }}</h3>
          <p class="record-note">{{ current.decisionNote || "—" }}</p></template
        >
        <template v-else-if="section === 'transfers'"
          ><div class="detail-grid">
            <div>
              <small>{{ t("移出蜂场", "Origin") }}</small
              >{{ snapshot.sourceName || siteName(current.sourceId) }}
            </div>
            <div>
              <small>{{ t("目的蜂场", "Destination") }}</small
              >{{ snapshot.destinationName || siteName(current.destinationId) }}
            </div>
            <div>
              <small>{{ t("计划日期", "Planned") }}</small
              >{{ current.plannedOn }}
            </div>
            <div>
              <small>{{ t("移出人 / 日期", "Sender / departure") }}</small
              >{{ personName(current.senderId) }} /
              {{ current.departedOn || "—" }}
            </div>
            <div>
              <small>{{ t("接收人 / 日期", "Receiver / receipt") }}</small
              >{{ personName(current.receiverId) }} /
              {{ current.receivedOn || "—" }}
            </div>
            <div>
              <small>{{ t("清单数量", "Manifest size") }}</small
              >{{ current.colonyIds.length }}
            </div>
          </div>
          <p class="record-note">{{ current.notes }}</p>
          <h3>{{ t("交接清单", "Handover manifest") }}</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ t("编号", "Code") }}</th>
                  <th>{{ t("蜂群", "Colony") }}</th>
                  <th>{{ t("移出时蜂场", "Origin at release") }}</th>
                  <th>{{ t("蜂王编号", "Queen code") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="c in snapshot.colonies ||
                  current.colonyIds.map((id) => ({ id, code: colonyName(id) }))"
                  :key="c.id"
                >
                  <td>{{ c.code }}</td>
                  <td>{{ c.name || "—" }}</td>
                  <td>{{ c.apiaryName || siteName(current.sourceId) }}</td>
                  <td>{{ c.queenCode || "—" }}</td>
                </tr>
              </tbody>
            </table>
          </div></template
        >
      </div>
    </template>
    <div v-else class="panel">
      <div class="toolbar">
        <label class="search"
          ><Search :size="16" /><input
            v-model="q"
            :placeholder="t('搜索编号、名称、说明', 'Search code, name, note')"
            :aria-label="t('搜索', 'Search')" /></label
        ><select v-model="state" :aria-label="t('状态筛选', 'State filter')">
          <option value="">{{ t("全部状态", "All states") }}</option>
          <option v-for="s in statuses" :key="s" :value="s">
            {{ labelState(s) }}
          </option></select
        ><select v-model="sort" :aria-label="t('排序', 'Sort')">
          <option value="newest">{{ t("最新在前", "Newest first") }}</option>
          <option value="oldest">{{ t("最早在前", "Oldest first") }}</option>
          <option value="name">
            {{ t("编号 / 名称", "Code / name") }}
          </option></select
        ><span class="muted"
          >{{ filtered.length }} {{ t("条", "records") }}</span
        >
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ t("编号 / 名称", "Code / name") }}</th>
              <template v-if="section === 'apiaries'"
                ><th>{{ t("位置", "Location") }}</th>
                <th>{{ t("容量", "Capacity") }}</th></template
              ><template v-else-if="section === 'colonies'"
                ><th>{{ t("当前蜂场", "Apiary") }}</th>
                <th>{{ t("管护人", "Keeper") }}</th>
                <th>{{ t("最新事实日期", "Latest date") }}</th></template
              ><template v-else-if="section === 'inspections'"
                ><th>{{ t("观察日期", "Observed") }}</th>
                <th>{{ t("记录人 / 复核人", "Inspector / reviewer") }}</th>
                <th>{{ t("蜂覆盖 / 子脾", "Covered / brood") }}</th></template
              ><template v-else
                ><th>{{ t("移出 → 接收蜂场", "Origin → destination") }}</th>
                <th>{{ t("计划日期", "Planned") }}</th>
                <th>{{ t("蜂群数", "Colonies") }}</th></template
              >
              <th>{{ t("状态", "State") }}</th>
              <th>{{ t("操作", "Actions") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in visible" :key="r.id">
              <td>
                <strong>{{
                  section === "inspections" ? colonyName(r.colonyId) : r.code
                }}</strong
                ><small class="muted"> {{ r.name || "#" + r.id }}</small>
              </td>
              <template v-if="section === 'apiaries'"
                ><td>{{ r.location }}</td>
                <td>{{ r.capacity }}</td></template
              ><template v-else-if="section === 'colonies'"
                ><td>{{ siteName(r.apiaryId) }}</td>
                <td>{{ personName(r.keeperId) }}</td>
                <td>{{ r.lastOn }}</td></template
              ><template v-else-if="section === 'inspections'"
                ><td>{{ r.observedOn }}</td>
                <td>
                  {{ personName(r.inspectorId) }} /
                  {{ personName(r.reviewerId) }}
                </td>
                <td>{{ r.coveredFrames }} / {{ r.broodFrames }}</td></template
              ><template v-else
                ><td>
                  {{ siteName(r.sourceId) }} → {{ siteName(r.destinationId) }}
                </td>
                <td>{{ r.plannedOn }}</td>
                <td>{{ r.colonyIds.length }}</td></template
              >
              <td>
                <span class="badge" :class="r.state">{{
                  r.state
                    ? stateName(r.state)
                    : labelState(r.enabled ? "ENABLED" : "DISABLED")
                }}</span
                ><small v-if="r.moveId" class="muted"> #{{ r.moveId }}</small>
              </td>
              <td>
                <div class="actions">
                  <button
                    v-if="section !== 'apiaries'"
                    :disabled="pending"
                    @click="openDetail(r)"
                  >
                    {{ t("详情", "Details") }}</button
                  ><button
                    v-if="canEdit(r)"
                    class="icon-button"
                    :disabled="pending"
                    :aria-label="t('编辑', 'Edit') + ' ' + (r.code || r.id)"
                    @click="edit(r)"
                  >
                    <Pencil :size="16" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!ready" class="empty">{{ t("读取中…", "Loading…") }}</div>
        <div v-else-if="!filtered.length" class="empty">
          {{ t("暂无匹配记录", "No matching records") }}
        </div>
      </div>
      <footer class="pagination">
        <span
          >{{ page }} / {{ Math.max(1, Math.ceil(filtered.length / 15)) }}</span
        ><button :disabled="page <= 1" @click="page--">
          {{ t("上一页", "Previous") }}</button
        ><button :disabled="page * 15 >= filtered.length" @click="page++">
          {{ t("下一页", "Next") }}
        </button>
      </footer>
    </div>
    <div v-if="editing" class="modal">
      <section class="wide" role="dialog" aria-modal="true" :aria-label="title">
        <header>
          <h2>
            {{
              editing.id
                ? t("编辑", "Edit")
                : editing.supersedesId
                  ? t("追加修订", "Append correction")
                  : t("新增", "Add")
            }}
            · {{ title }}
          </h2>
          <button
            :disabled="pending"
            :aria-label="t('关闭', 'Close')"
            @click="editing = null"
          >
            <X :size="20" />
          </button>
        </header>
        <form @submit.prevent="save">
          <div class="form-grid">
            <component
              :is="f.type === 'manifest' ? 'fieldset' : 'label'"
              v-for="f in fields"
              :key="f.key"
              :class="f.type === 'checkbox' ? 'check' : ''"
              >{{ f.label
              }}<input
                v-if="['text', 'number', 'date', 'checkbox'].includes(f.type)"
                v-model="editing[f.key]"
                :type="f.type"
                :required="f.type !== 'checkbox'"
                :readonly="f.readonly"
                :disabled="pending"
                :min="f.min"
                :max="f.max"
                :maxlength="f.maxlength || 120"
                :step="f.type === 'number' ? 1 : undefined"
              /><textarea
                v-else-if="f.type === 'textarea'"
                v-model="editing[f.key]"
                required
                rows="4"
                :maxlength="f.maxlength"
                :disabled="pending"
              ></textarea
              ><select
                v-else-if="f.type === 'site'"
                v-model="editing[f.key]"
                required
                :disabled="pending || f.readonly"
              >
                <option value="">{{ t("选择蜂场", "Select apiary") }}</option>
                <option
                  v-for="s in options.apiaries.filter(
                    (s) => s.enabled || s.id === Number(editing[f.key]),
                  )"
                  :key="s.id"
                  :value="s.id"
                >
                  {{ s.code }} · {{ s.name }}
                </option></select
              ><select
                v-else-if="f.type === 'person'"
                v-model="editing[f.key]"
                required
                :disabled="pending"
              >
                <option value="">{{ t("选择人员", "Select person") }}</option>
                <option
                  v-for="person in candidates(f)"
                  :key="person.id"
                  :value="person.id"
                >
                  {{ person.displayName }}
                </option></select
              ><select
                v-else-if="f.type === 'colony'"
                v-model="editing[f.key]"
                required
                :disabled="pending || f.readonly"
              >
                <option value="">
                  {{ t("选择本人蜂群", "Select own colony") }}
                </option>
                <option
                  v-for="c in colonies.filter(
                    (c) =>
                      c.id === Number(editing.colonyId) ||
                      (c.keeperId === me.id &&
                        c.state === 'ACTIVE' &&
                        !c.moveId),
                  )"
                  :key="c.id"
                  :value="c.id"
                >
                  {{ c.code }} · {{ c.name }}
                </option></select
              ><select
                v-else-if="f.type === 'category'"
                v-model="editing[f.key]"
                required
                :disabled="pending"
              >
                <option
                  v-for="c in options.categories.filter(
                    (c) => c.enabled || c.code === editing.category,
                  )"
                  :key="c.id"
                  :value="c.code"
                >
                  {{ c.name }} / {{ c.nameEn }}
                </option></select
              ><select
                v-else-if="f.type === 'choice'"
                v-model="editing[f.key]"
                :disabled="pending"
              >
                <option
                  v-for="choice in f.choices"
                  :key="choice[0]"
                  :value="choice[0]"
                >
                  {{ choice[1] }}
                </option></select
              ><span v-else-if="f.type === 'manifest'"
                ><label v-for="c in eligible" :key="c.id" class="check"
                  ><input
                    v-model="editing.colonyIds"
                    type="checkbox"
                    :value="c.id"
                    :disabled="pending"
                  />{{ c.code }} · {{ c.name }}</label
                ><small v-if="!eligible.length" class="muted">{{
                  t(
                    "当前蜂场没有可转场蜂群",
                    "No eligible colonies at this origin",
                  )
                }}</small></span
              ></component
            >
          </div>
          <p v-if="editing.supersedesId" class="hint">
            {{ t("修订目标", "Corrects") }} #{{ editing.supersedesId }} ·
            {{ t("原核实事实保留", "Original verified facts retained") }}
          </p>
          <footer>
            <button type="button" :disabled="pending" @click="editing = null">
              {{ t("取消", "Cancel") }}</button
            ><button class="primary" :disabled="pending">
              {{ t("保存", "Save") }}
            </button>
          </footer>
        </form>
      </section>
    </div>
    <div v-if="acting" class="modal">
      <section
        role="dialog"
        aria-modal="true"
        :aria-label="actionName(acting.action)"
      >
        <header>
          <h2>{{ actionName(acting.action) }}</h2>
          <button :disabled="pending" @click="acting = null">
            <X :size="20" />
          </button>
        </header>
        <form @submit.prevent="performAction">
          <label
            v-if="
              ['split', 'requeen', 'retire', 'depart', 'receive'].includes(
                acting.action,
              )
            "
            >{{ t("实际日期", "Actual date")
            }}<input
              v-model="acting.happenedOn"
              type="date"
              required
              :disabled="pending" /></label
          ><template v-if="acting.action === 'split'"
            ><label
              >{{ t("新蜂群编号", "New colony code")
              }}<input
                v-model="acting.code"
                required
                maxlength="30"
                :disabled="pending" /></label
            ><label
              >{{ t("新蜂群名称", "New colony name")
              }}<input
                v-model="acting.name"
                required
                maxlength="120"
                :disabled="pending" /></label></template
          ><label v-if="acting.action === 'requeen'"
            >{{ t("新蜂王编号", "New queen code")
            }}<input
              v-model="acting.queenCode"
              required
              maxlength="60"
              :disabled="pending" /></label
          ><label v-if="!['submit', 'release'].includes(acting.action)"
            >{{ t("事实与办理说明", "Facts & action note")
            }}<textarea
              v-model="acting.note"
              required
              minlength="4"
              maxlength="500"
              rows="4"
              :disabled="pending"
            ></textarea>
          </label>
          <p v-if="acting.action === 'release'" class="hint">
            {{
              t(
                "发布后冻结清单并预留蜂群及目标容量。",
                "Release freezes the manifest and reserves colonies and destination capacity.",
              )
            }}
          </p>
          <footer>
            <button type="button" :disabled="pending" @click="acting = null">
              {{ t("取消", "Cancel") }}</button
            ><button class="primary" :disabled="pending">
              {{ actionName(acting.action) }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </section>
</template>
