// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import { ref } from "vue";
export const language = ref("zh");
/** 切换文案，不改写业务观察。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const t = (zh, en) => (language.value === "zh" ? zh : en);
const states = {
  DRAFT: ["草稿", "Draft"],
  PENDING: ["待独立复核", "In review"],
  VERIFIED: ["已核实", "Verified"],
  CANCELLED: ["已取消", "Cancelled"],
  ACTIVE: ["在场管护", "Active"],
  IN_TRANSIT: ["转场途中", "In transit"],
  RETIRED: ["已退出", "Retired"],
  RELEASED: ["待移出", "Released"],
  RECEIVED: ["已接收", "Received"],
};
/** 仅映射实际服务端状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const stateName = (state) =>
  states[state] ? t(...states[state]) : state;
const errors = {
  UNAUTHENTICATED: [
    "会话失效，请重新登录。",
    "Session expired. Sign in again.",
  ],
  LOGIN_FAILED: [
    "账号、密码错误或已停用。",
    "Invalid credentials or disabled account.",
  ],
  LOGIN_THROTTLED: [
    "尝试过多，五分钟后重试。",
    "Too many attempts. Retry in five minutes.",
  ],
  FORBIDDEN: ["没有操作权限。", "Permission denied."],
  OUT_OF_SCOPE: [
    "记录不在当前团队、本人或指派范围。",
    "Outside your team, own or assigned scope.",
  ],
  VERSION_CONFLICT: [
    "记录已变化，请刷新核对后再提交。",
    "Record changed. Refresh before submitting.",
  ],
  STATE_INVALID: ["当前状态不能办理。", "Action unavailable in this state."],
  COLONY_BUSY: [
    "蜂群已退出或正在转场，不能办理。",
    "Colony retired or reserved/in transit.",
  ],
  INDEPENDENT_REQUIRED: [
    "复核人不能是记录人，移出人与接收人也须不同。",
    "Reviewer must differ from inspector; sender and receiver must differ.",
  ],
  REVIEWER_ONLY: [
    "仅指定复核人可以决定。",
    "Only the assigned reviewer may decide.",
  ],
  KEEPER_ONLY: [
    "仅当前指定管护人可以记录。",
    "Only the assigned keeper may record.",
  ],
  OPERATOR_ONLY: [
    "仅指定移出人或接收人可以办理。",
    "Only the assigned sender/receiver may act.",
  ],
  WORKER_UNAVAILABLE: [
    "相关账号、团队或岗位权限不匹配。",
    "Account, team or permission incompatible.",
  ],
  INSPECTION_PENDING: [
    "仍有未处理巡检，先完成或撤回。",
    "Resolve pending inspections first.",
  ],
  FRAME_INVALID: [
    "子脾框数不能超过蜂覆盖框数。",
    "Brood frames cannot exceed covered frames.",
  ],
  DATE_INVALID: [
    "实际日期不能在未来，支持2000–2100年。",
    "Actual date cannot be future; supported years 2000–2100.",
  ],
  DATE_ORDER: [
    "日期早于已有业务日期；修订保持原观察日期。",
    "Date precedes existing facts; corrections retain the original date.",
  ],
  REVISION_INVALID: [
    "修订目标不属于本蜂群或已被代替。",
    "Correction target invalid or already superseded.",
  ],
  CAPACITY_EXCEEDED: [
    "目标蜂场容量不足，已发布接收也占预留。",
    "Capacity exceeded, including released incoming reservations.",
  ],
  SITE_INVALID: [
    "蜂场、团队或清单位置不匹配。",
    "Apiary, team or manifest mismatch.",
  ],
  SITE_DISABLED: ["蜂场已停用。", "Apiary disabled."],
  SNAPSHOT_STALE: [
    "蜂群位置或交接预留已变化。",
    "Colony location or reservation changed.",
  ],
  SNAPSHOT_CHANGED: ["快照校验不一致，操作拒绝。", "Snapshot digest mismatch."],
  IDENTITY_LOCKED: [
    "稳定身份或日期不能改写。",
    "Stable identity/date cannot be changed.",
  ],
  REFERENCED: [
    "已有业务引用，历史不能删除或停用。",
    "Referenced history prevents deletion/disabling.",
  ],
  CONFLICT: ["编号重复或仍被引用。", "Duplicate code or referenced record."],
  DUPLICATE_COLONY: ["同一蜂群只能添加一次。", "Add each colony once."],
  LINE_LIMIT: [
    "转场清单须包含1–50个蜂群。",
    "A transfer requires 1–50 colonies.",
  ],
  RESOURCE_LIMIT: ["已到系统记录上限。", "Resource limit reached."],
  CATEGORY_INVALID: ["分类缺失或已停用。", "Category missing or disabled."],
  INVALID_INPUT: ["字段格式或必填值不正确。", "Invalid or missing fields."],
  NOTE_REQUIRED: [
    "说明至少4个字符。",
    "Enter a note of at least four characters.",
  ],
  CODE_INVALID: [
    "编号用2–30位大写字母、数字或下划线。",
    "Use 2–30 uppercase letters, digits or underscores.",
  ],
  PASSWORD_WEAK: [
    "密码需12–72字节，包含大小写字母及数字。",
    "Use 12–72 bytes, uppercase, lowercase and digits.",
  ],
  LAST_ADMIN: ["须保留一名完整管理员。", "Keep one full administrator."],
  ACCOUNT_ASSIGNED: [
    "已有业务历史，不能迁移账号团队。",
    "Business references prevent team reassignment.",
  ],
  OLD_PASSWORD_INVALID: ["原密码错误。", "Current password incorrect."],
  RESULT_UNKNOWN: [
    "提交结果未知，请刷新核对，勿重复提交。",
    "Result unknown. Refresh and check before repeating.",
  ],
  NETWORK_ERROR: ["连接失败，请刷新。", "Connection failed. Refresh."],
};
/** 明确失败，不把网络错误当成功。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const errorMessage = (error) =>
  errors[error.message]
    ? t(...errors[error.message])
    : t("操作未完成：", "Action failed: ") + error.message;
/** 表单不直接改写列表。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const cloneRecord = (record) => JSON.parse(JSON.stringify(record));
/** 时间仅用于显示，业务日期由团队时区服务端校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export const time = (value) => (value ? new Date(value).toLocaleString() : "");
