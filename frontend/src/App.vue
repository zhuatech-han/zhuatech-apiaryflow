<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, onMounted, watch } from "vue";
import {
  BookOpen,
  ClipboardList,
  Truck,
  ChartNoAxesCombined,
  Users,
  Shield,
  Settings,
  History,
  LogOut,
  Globe,
  Menu,
  KeyRound,
  X,
} from "@lucide/vue";
import { api, resetApi } from "./api.js";
import { t, language, errorMessage, time, stateName } from "./ui.js";
import AdminPanel from "./components/AdminPanel.vue";
import BusinessPanel from "./components/BusinessPanel.vue";
const me = ref(null),
  section = ref(""),
  pending = ref(false),
  ready = ref(false),
  error = ref(""),
  notice = ref(""),
  mobile = ref(false),
  passwordDialog = ref(false);
const login = ref({ username: "", password: "" }),
  password = ref({ oldPassword: "", newPassword: "" });
const report = ref(null),
  audit = ref([]),
  about = ref(false);
const icons = {
  colonies: BookOpen,
  inspections: ClipboardList,
  transfers: Truck,
  apiaries: BookOpen,
  reports: ChartNoAxesCombined,
  users: Users,
  roles: Shield,
  settings: Settings,
  audit: History,
};
/** 错误不会转换为成功；身份失效后清理浏览器业务状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
function onError(e) {
  notice.value = "";
  error.value = errorMessage(e);
  if (e.status === 401 && e.message !== "LOGIN_FAILED") {
    me.value = null;
    resetApi();
    section.value = "";
  }
}
/** 重新读取实时菜单，只允许拥有权限的页面。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function profile() {
  me.value = await api("/auth/me");
  if (!me.value.menus.some((m) => m.code === section.value))
    section.value = me.value.menus[0]?.code || "";
}
/** 写操作禁止双击和自动重试，成功后核对当前角色。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function perform(path, method, body) {
  if (pending.value) return null;
  pending.value = true;
  error.value = "";
  notice.value = "";
  try {
    const r = await api(path, { method, body });
    await profile();
    notice.value = t("已保存", "Saved");
    return r;
  } catch (e) {
    onError(e);
    return null;
  } finally {
    pending.value = false;
  }
}
/** 登录不缓存密码，登录后刷新会话CSRF。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function signIn() {
  if (pending.value) return;
  pending.value = true;
  error.value = "";
  try {
    me.value = await api("/auth/login", { method: "POST", body: login.value });
    login.value.password = "";
    resetApi();
    section.value = me.value.menus[0]?.code || "";
  } catch (e) {
    onError(e);
  } finally {
    pending.value = false;
  }
}
/** 退出使服务端会话失效并清除浏览器身份。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function signOut() {
  if (pending.value) return;
  pending.value = true;
  try {
    await api("/auth/logout", { method: "POST" });
    error.value = "";
    notice.value = "";
    me.value = null;
    section.value = "";
    report.value = null;
    audit.value = [];
    password.value = { oldPassword: "", newPassword: "" };
    resetApi();
  } catch (e) {
    onError(e);
  } finally {
    pending.value = false;
  }
}
/** 自助修改密码后必须重新登录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function changePassword() {
  if (pending.value) return;
  pending.value = true;
  try {
    await api("/auth/password", { method: "POST", body: password.value });
    me.value = null;
    passwordDialog.value = false;
    password.value = { oldPassword: "", newPassword: "" };
    resetApi();
    notice.value = t(
      "密码已修改，请重新登录。",
      "Password changed. Sign in again.",
    );
  } catch (e) {
    onError(e);
  } finally {
    pending.value = false;
  }
}
async function loadView() {
  if (me.value) {
    error.value = "";
    notice.value = "";
  }
  mobile.value = false;
  if (!me.value) return;
  try {
    if (section.value === "reports") report.value = await api("/reports");
    if (section.value === "audit") audit.value = await api("/audit");
  } catch (e) {
    onError(e);
  }
}
watch(section, loadView);
onMounted(async () => {
  try {
    await profile();
  } catch (e) {
    if (e.status !== 401) onError(e);
  } finally {
    ready.value = true;
  }
});
</script>
<template>
  <div v-if="!ready" class="loading">{{ t("正在连接…", "Connecting…") }}</div>
  <main v-else-if="!me" class="login-layout">
    <section class="login-intro">
      <img src="/brand/logo.jpg" alt="知华科技 ZhiHua Technology" />
      <p class="eyebrow">APIARYFLOW · 1.0.0</p>
      <h1>
        {{ t("蜂场与蜂群\n记录工作台", "Apiary & colony\nrecord workspace") }}
      </h1>
      <p>
        {{
          t(
            "蜂群台账 · 巡检复核 · 转场交接",
            "Colony records · Inspections · Transfers",
          )
        }}
      </p>
      <div class="login-flow">
        <span>01 {{ t("建档", "Register") }}</span
        ><span>02 {{ t("观察", "Inspect") }}</span
        ><span>03 {{ t("交接", "Handover") }}</span>
      </div>
    </section>
    <section class="login-card">
      <Globe class="language-icon" /><button
        class="language"
        @click="language = language === 'zh' ? 'en' : 'zh'"
      >
        {{ language === "zh" ? "English" : "中文" }}
      </button>
      <h2>{{ t("登录工作台", "Sign in") }}</h2>
      <p class="muted">
        {{ t("使用所属团队的工作账号", "Use your team account") }}
      </p>
      <form @submit.prevent="signIn">
        <label
          >{{ t("账号", "Account")
          }}<input
            v-model="login.username"
            autocomplete="username"
            required
            maxlength="60"
            :disabled="pending" /></label
        ><label
          >{{ t("密码", "Password")
          }}<input
            v-model="login.password"
            autocomplete="current-password"
            type="password"
            required
            maxlength="128"
            :disabled="pending"
        /></label>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <p v-if="notice" role="status">{{ notice }}</p>
        <button class="primary" :disabled="pending">
          {{ pending ? t("登录中…", "Signing in…") : t("登录", "Sign in") }}
        </button>
      </form>
      <div class="login-license">
        <p>
          {{
            t(
              "知华科技（上海如静知华信息科技有限公司）",
              "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
            )
          }}
        </p>
        <p>
          {{
            t(
              "源码公开、非商业学习使用；未经书面授权不得商用。",
              "Source available for personal non-commercial learning. Commercial use requires written authorization.",
            )
          }}
        </p>
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
          >{{ t("官网与商业咨询", "Website & commercial enquiries") }} ↗</a
        >
        <p v-if="language === 'zh'">微信 zhuatech / zhuatech2</p>
      </div>
    </section>
  </main>
  <div v-else class="shell">
    <aside :class="{ open: mobile }">
      <div class="brand">
        <img src="/brand/logo.jpg" alt="知华科技" />
        <div>
          <strong>ApiaryFlow</strong
          ><small>{{ t("蜂群巡检与转场", "Inspections & transfers") }}</small>
        </div>
      </div>
      <div class="nav-label">{{ t("工作台", "WORKSPACE") }}</div>
      <nav>
        <button
          v-for="m in me.menus"
          :key="m.id"
          :class="{ active: section === m.code }"
          :disabled="pending"
          @click="section = m.code"
        >
          <component :is="icons[m.code] || BookOpen" :size="18" /><span>{{
            language === "zh" ? m.name : m.nameEn
          }}</span>
        </button>
      </nav>
      <div class="sidebar-footer">
        <button @click="about = true">
          ApiaryFlow 1.0.0 · {{ t("关于", "About") }}</button
        ><a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
          >{{ t("知华科技官网", "ZhiHua Technology") }} ↗</a
        ><small v-if="language === 'zh'">商业咨询 zhuatech / zhuatech2</small>
      </div>
    </aside>
    <div v-if="mobile" class="nav-shade" @click="mobile = false"></div>
    <main class="workspace">
      <header class="topbar">
        <button
          class="mobile-toggle"
          aria-label="菜单 Menu"
          @click="mobile = !mobile"
        >
          <Menu :size="20" /></button
        ><span class="muted">{{ t("蜂场协同", "Apiary workspace") }}</span>
        <div class="account">
          <span
            >{{ me.displayName }} <small>{{ me.role }}</small></span
          ><button
            :disabled="pending"
            @click="language = language === 'zh' ? 'en' : 'zh'"
          >
            <Globe :size="17" />{{ language === "zh" ? "EN" : "中文" }}</button
          ><button
            :disabled="pending"
            :aria-label="t('修改密码', 'Change password')"
            @click="passwordDialog = true"
          >
            <KeyRound :size="17" /></button
          ><button
            :disabled="pending"
            :aria-label="t('退出', 'Sign out')"
            @click="signOut"
          >
            <LogOut :size="17" />
          </button>
        </div>
      </header>
      <div class="content">
        <div v-if="error" class="error toast" role="alert">
          {{ error
          }}<button :aria-label="t('关闭', 'Close')" @click="error = ''">
            <X :size="16" />
          </button>
        </div>
        <div v-if="notice" class="notice" role="status">{{ notice }}</div>
        <BusinessPanel
          v-if="
            ['colonies', 'inspections', 'transfers', 'apiaries'].includes(
              section,
            )
          "
          :key="section + '-' + me.id"
          :section="section"
          :me="me"
          :pending="pending"
          :perform="perform"
          @error="onError"
        />
        <AdminPanel
          v-else-if="['users', 'roles', 'settings'].includes(section)"
          :section="section"
          :pending="pending"
          :perform="perform"
          @error="onError"
        />
        <section v-else-if="section === 'reports'">
          <header class="page-heading">
            <div>
              <p class="eyebrow">OPERATIONS</p>
              <h1>{{ t("蜂群与交接统计", "Colonies & handovers") }}</h1>
              <p class="muted">
                {{
                  t(
                    "仅汇总授权范围内的实际记录",
                    "Actual records within your access scope",
                  )
                }}
              </p>
            </div>
            <button :disabled="pending" @click="loadView">
              {{ t("刷新", "Refresh") }}
            </button>
          </header>
          <template v-if="report"
            ><div class="metrics">
              <article>
                <small>{{ t("蜂群记录", "Colony records") }}</small
                ><strong>{{ report.colonies }}</strong>
              </article>
              <article>
                <small>{{
                  t("有效核实巡检", "Current verified inspections")
                }}</small
                ><strong>{{ report.verified }}</strong>
              </article>
              <article>
                <small>{{ t("已接收转场", "Received transfers") }}</small
                ><strong>{{ report.received }}</strong>
              </article>
            </div>
            <div class="panel">
              <h2>{{ t("蜂群状态", "Colony status") }}</h2>
              <div
                v-for="(count, state) in report.byState"
                :key="state"
                class="state-row"
              >
                <span>{{ stateName(state) }}</span
                ><meter
                  :value="count"
                  :max="Math.max(report.colonies, 1)"
                ></meter
                ><strong>{{ count }}</strong>
              </div>
              <p class="hint">
                {{ t("待复核巡检", "Pending inspections") }}:
                {{ report.pending }} ·
                {{ t("途中交接", "Transfers in transit") }}:
                {{ report.inTransit }}
              </p>
            </div>
            <div class="panel">
              <h2>{{ t("当前蜂场分布", "Current apiary distribution") }}</h2>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>{{ t("蜂场", "Apiary") }}</th>
                      <th>{{ t("当前蜂群", "Current colonies") }}</th>
                      <th>{{ t("场地容量", "Site capacity") }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="site in report.sites" :key="site.code">
                      <td>
                        {{ site.name }}
                        <small class="mono">{{ site.code }}</small>
                      </td>
                      <td>{{ site.colonies }}</td>
                      <td>{{ site.capacity }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p class="hint">
                {{
                  t(
                    "途中蜂群仍显示原位置，接收后才迁移；本人范围显示本人可见蜂群，不代表全场占用。",
                    "In-transit colonies retain the origin until received. Assigned scope shows visible colonies, not total occupancy.",
                  )
                }}
              </p>
            </div></template
          >
        </section>
        <section v-else-if="section === 'audit'">
          <header class="page-heading">
            <div>
              <p class="eyebrow">AUDIT</p>
              <h1>{{ t("审计记录", "Audit trail") }}</h1>
            </div>
            <button :disabled="pending" @click="loadView">
              {{ t("刷新", "Refresh") }}
            </button>
          </header>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ t("时间", "Time") }}</th>
                  <th>{{ t("账号", "Account") }}</th>
                  <th>{{ t("操作", "Action") }}</th>
                  <th>{{ t("对象", "Object") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in audit.slice(0, 200)" :key="r.id">
                  <td>{{ time(r.createdAt) }}</td>
                  <td>{{ r.actor }}</td>
                  <td>{{ r.action }}</td>
                  <td>#{{ r.objectId }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="hint">
            {{
              t(
                "显示最新200条，日志不包含密码或业务正文。",
                "Latest 200 entries. No passwords or business payloads.",
              )
            }}
          </p>
        </section>
        <p v-else class="empty">
          {{
            t("当前账号没有启用的菜单。", "No enabled menus for this account.")
          }}
        </p>
      </div>
    </main>
  </div>
  <div
    v-if="passwordDialog && me"
    class="modal"
    @click.self="!pending && (passwordDialog = false)"
  >
    <section
      role="dialog"
      aria-modal="true"
      :aria-label="t('修改密码', 'Change password')"
    >
      <header>
        <h2>{{ t("修改密码", "Change password") }}</h2>
        <button :disabled="pending" @click="passwordDialog = false">
          <X :size="18" />
        </button>
      </header>
      <form @submit.prevent="changePassword">
        <label
          >{{ t("原密码", "Current password")
          }}<input
            v-model="password.oldPassword"
            type="password"
            required
            autocomplete="current-password"
            :disabled="pending" /></label
        ><label
          >{{ t("新密码", "New password")
          }}<input
            v-model="password.newPassword"
            type="password"
            required
            autocomplete="new-password"
            :disabled="pending"
            maxlength="72"
        /></label>
        <p class="hint">
          {{
            t(
              "12–72 UTF-8字节，包含大小写字母和数字。",
              "12–72 UTF-8 bytes, uppercase, lowercase and digits.",
            )
          }}
        </p>
        <button class="primary" :disabled="pending">
          {{ t("修改并退出", "Change and sign out") }}
        </button>
      </form>
    </section>
  </div>
  <div v-if="about" class="modal" @click.self="about = false">
    <section
      role="dialog"
      aria-modal="true"
      :aria-label="t('关于系统', 'About')"
    >
      <header>
        <h2>ApiaryFlow 1.0.0</h2>
        <button @click="about = false"><X :size="18" /></button>
      </header>
      <p>
        {{
          t(
            "知华科技（上海如静知华信息科技有限公司）",
            "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
          )
        }}
      </p>
      <p>
        {{
          t(
            "公开源码学习版；仅限个人学习交流，未经书面授权不得商用。",
            "Source available for personal non-commercial learning. Written authorization is required for commercial use.",
          )
        }}
      </p>
      <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
        >https://www.zhuatech.cn/</a
      >
      <p v-if="language === 'zh'">
        商业授权或深度定制开发请联系知华科技：微信 zhuatech / zhuatech2
      </p>
      <p v-else>
        <a href="mailto:han@zhuatech.cn">han@zhuatech.cn</a> ·
        <a href="mailto:jack@zhuatech.cn">jack@zhuatech.cn</a> ·
        <a href="https://wa.me/8617521234993">WhatsApp +86 17521234993</a>
      </p>
    </section>
  </div>
</template>
