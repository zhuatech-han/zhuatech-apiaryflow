# ApiaryFlow 验收 / Verification

知华科技（上海如静知华信息科技有限公司）· https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2。

## 检查步骤 / Gates

1. 后端Spotless、单元及HTTP/Security/JPA/Flyway集成测试；前端格式、ESLint、请求安全测试和生产构建；Python格式与编译。
2. Compose配置与完整镜像构建，Docker Maven执行全部测试；全新MySQL8.4卷启动、V1/V2、Hibernate结构验证、三个健康检查、管理员登录。
3. 隔离库真实HTTP检查完整建档、巡检退回/核实/修订、分群、換王、发布预留、移出/接收、取消、退出、原历史CSV不变、并发200/409、CSRF及团队/本人/指定人隔离。
4. 浏览器实际操作、管理页面、导出、中文英文手机与真实截图。检查图片无客户资料、密码、密钥或Apple密码弹窗。
5. 采集全部目录、业务、事件、非LOGIN审计和CSV，重启后精确比较；私有一致性备份后在另一个全新卷恢复并精确比较。重新登录产生的LOGIN审计单独排除。
6. 检查源码署名、官网、两个原始微信二维码、README互链/图片/英文联系方式、LICENSE和敏感信息；发布后逐平台核对公开状态、全部路径/文件模式/blob及SHA。

H2integration tests supplement rather than replace actual MySQL checks. Only isolated fictitious TEST data are used. Local acceptance does not certify production suitability, disease diagnosis, husbandry, food quality or legal compliance.

## 实际结果 / Results

2026-10-11，本版在隔离TEST环境完成以下检查：

- 后端31项：6项安全输入规则、25项HTTP/Security/JPA/Flyway集成；0失败、0错误、0跳过。前端7项请求安全测试、Prettier、ESLint及生产构建通过；Python Black与编译检查通过。
- 完整Docker构建执行全部测试。最终代码在全新MySQL8.4卷启动，V1/V2、Hibernate结构验证、三服务健康和初始化登录通过；真实业务152项检查通过。
- 巡检退回／核实／追加修订、原蜂场与蜂王上下文、分群、换王、发布预留、双人移出接收、取消与退出通过；并发相同版本实测恰好200／409。已核实原始CSV在修订和转场后保持不变。
- 浏览器实际登记9／6框观察并由指定独立复核人核实；实际编制、发布和双人办理TEST_GUI_MOVE，最终RECEIVED／版本4，蜂群接收后位于目标蜂场且预留清除。实际浏览器CSV下载476字节与授权服务端导出完全相同。
- 全部目录、4个蜂群、3份巡检、3次转场、事件、非LOGIN审计和CSV共29组数据；最终采集37项，重启及独立全新恢复各38项检查通过，精确一致。仅排除重新登录产生的LOGIN审计。
- 10张当前运行截图已实拍、逐张检查；手机390×844页面宽390、关闭侧栏右边界0。历史说明列修正为841.5像素后复验重拍。没有可见Apple密码弹窗、真实客户资料或凭据。
- 旧会话CSRF拒绝与真正权限拒绝分别测试，不自动重放写入；实际后端重启后旧浏览器会话返回登录并显示失效提示。
- 两张原始微信二维码和正式LOGO字节保留；中英文互链、图片、联系方式、官网HTTPS200、非商业LICENSE及84个公开文件静态检查通过。npm官方审计检查时0条已知漏洞，不保证没有未知风险。

These checks validate isolated local deployments and fictitious TEST data, not production suitability or disease/husbandry/food/legal certification. Backend31 and frontend7 tests passed, along with actual MySQL152 business checks, browser inspection and two-person handover,476-byte native CSV equality, and38-check restart/new-restore comparisons covering29 persisted sets. All10 running screenshots were visually reviewed.
