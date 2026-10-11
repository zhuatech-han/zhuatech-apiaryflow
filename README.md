# ApiaryFlow · 知华蜂场蜂群巡检与转场记录

[中文](README.md) | [English](README.en.md)

![知华科技](frontend/public/brand/logo.jpg)

知华科技（上海如静知华信息科技有限公司）的**公开源码学习版／非商业源码版**。使用 Java 21、Spring Boot、Vue 3、MySQL 8.4，为小型蜂业团队、蜂场协作人员与教学实践提供蜂群身份、观察复核、分群记录和转场交接。官网：[https://www.zhuatech.cn/](https://www.zhuatech.cn/)。未经书面授权不得商用。

## 一群蜂，跨蜂场的连续记录

蜂箱所在位置会变，蜂群身份和已经核实的观察不能随之被改写。ApiaryFlow 将蜂群的稳定编号与当前位置分开，保留指定管护人、蜂王更换、分群关系、独立复核和移出／接收事实。适用于学习蜂场协作、代替分散表格进行非商业评估。

系统提供业务记录与描述性汇总；**不提供疾病诊断、用药建议、养殖处方、蜜质认证或法定申报**。观察的真实性和实际蜂场操作由具备相应能力的人员负责。

## 已实现的完整工作路径

1. 协调人员登记蜂场、所属团队、位置描述和最大容量，建立蜂群稳定编号、来源、分类与指定管护人。
2. 管护人记录本人蜂群的观察日期、蜂覆盖框数、子脾框数、是否见王、贮蜜及子脾观察和说明，选择独立复核人。
3. 送审时冻结当时的蜂群、蜂场和蜂王上下文；指定复核人核实或退回。管护人可撤回修改或取消草稿。
4. 已核实记录不能编辑、删除；修订创建新记录并引用原记录，保留原观察日期和当时蜂场。旧记录、摘要和原CSV继续可查。
5. 协调人员登记分群，新蜂群获得独立编号并关联父群；父群仍保留。换王事件保留旧／新编号与实际日期，退出蜂群保留历史。
6. 编制同团队转场，选择移出与接收蜂场、1–50个蜂群及不同的指定移出人和接收人。
7. 发布时冻结清单、预留蜂群和目标容量；仅指定移出人确认移出，蜂群进入途中状态。仅指定接收人确认接收后才改变当前位置。
8. 发布后可在实际移出前取消并解除预留；移出后不能取消或自动假设接收。预留／途中蜂群禁止巡检、换王、分群和其他转场。

| 工作空间 | 已实现内容 |
| --- | --- |
| 蜂群台账 | 建档、版本化维护、从未使用误建记录删除、指定管护、来源、分类、当前蜂场、分群关联、换王和退出事件 |
| 巡检与复核 | 本人记录、指定独立复核、退回／撤回／取消、冻结上下文、追加修订、原始和修订关系、CSV |
| 转场交接 | 草稿、发布、容量预留、蜂群排他预留、指定移出、指定接收、实际日期、取消、冻结清单及CSV |
| 蜂场目录 | 稳定身份、团队、位置、容量、启停；在场或预留引用防止错误停用与容量缩减 |
| 统计 | 授权范围内蜂群状态、有效核实巡检、待复核、已接收及途中交接、当前蜂场分布 |
| 管理端 | 账号、角色、接口权限、团队与IANA时区、菜单、分类字典、容量参数、审计、最后完整管理员保护 |

用户端提供按编号／名称／说明搜索、状态筛选、排序、分页、详情、表单、状态反馈；中文／英文界面和手机布局。管理端与业务端均连接真实后端及数据库，没有纯前端假成功模式。

## 岗位和边界

- 蜂场协调默认有团队范围，可维护蜂场／蜂群、编制转场和查看审计；蜂场维护需要团队或全部范围。
- 管护人员默认只有本人／指定范围。只有**当前指定管护人本人**可填写巡检；只有指定移出／接收人可办对应交接。管理员不能代办指定人流程。
- 复核人员默认只有指定记录范围，不能核实自己的观察。账号停用、团队停用、权限改变和密码重置在后续请求即时核验。
- 团队数据隔离。跨团队转场未实现；分群只表示录入的来源关系，不作遗传鉴定。
- 实际日期支持2000–2100年，不晚于所属团队时区的今天；普通记录不得早于已有实体业务日期。转场计划可为未来日期，移出不早于计划，接收不早于移出。修订保持原观察日期。
- 框数为0–200的整数，子脾框数不超过蜂覆盖框数；“未确定”与零数值分别记录，不推断疾病。
- 已发布的目标容量预留计入蜂场上限；移出中的蜂群在接收前仍保留原位置。数据范围为本人时，统计中的蜂场数量只表示本人可见蜂群，不是全场占用。
- 账号／角色／团队及核心业务校验版本；菜单／字典／参数串行保存，以最后保存者为准。全部写入在事务内串行化，适合单实例低容量协作；不宣称高吞吐多租户SaaS。容量参数100–1000条，不自动删除历史。

**未实现**：部分接收、运输异常处理、离线同步、地图定位、IoT、NFC、附件上传、自动预警、AI、诊断／用药、采蜜批次、销售、库存、财务及外部服务集成。无需外部业务账号、API密钥或付费服务。

## 当前运行页面

截图是独立验收环境明确标记的TEST录入数据，不是客户案例。

### 登录和蜂群工作台

![登录：使用团队工作账号](docs/screenshots/01-login.jpg)

![用户端：蜂群身份、当前位置和指定管护](docs/screenshots/02-colonies.jpg)

### 巡检事实和交接清单

![核心业务：独立复核与保留原记录的修订](docs/screenshots/03-inspection.jpg)

![转场：冻结蜂群清单和双方实际办理](docs/screenshots/04-transfer.jpg)

### 管理、统计和权限

![管理端：工作账号、团队和角色](docs/screenshots/05-accounts.jpg)

![统计：有效巡检、交接与蜂场分布](docs/screenshots/06-reports.jpg)

![权限：接口权限和团队／本人范围](docs/screenshots/07-roles.jpg)

[蜂群事件与分群](docs/screenshots/08-timeline.jpg) · [英文界面](docs/screenshots/09-english.jpg) · [手机页面](docs/screenshots/10-mobile.jpg)

## 架构与目录

浏览器 → Nginx同源代理 → Spring Security会话／CSRF → 事务服务与JPA → MySQL。Flyway管理V1身份及V2蜂场业务结构，Hibernate只验证结构。记录摘要使用SHA256；不能抵抗数据库管理员篡改，不宣称法定电子签名或合规证据系统。

```text
backend/       Java21 / Spring Boot4.0.7 / Security / JPA / Flyway
frontend/      Vue3.5.43 / Vite8.1.5 / 中英文业务和管理页面
scripts/       独立配置、真实业务检查、私有备份／全新恢复
docs/         业务边界、架构、接口、操作、部署、安全及运行截图
compose.yaml   MySQL8.4 + backend + Nginx前端
LICENSE        自定义非商业源码许可证
NOTICE         第三方许可边界
```

环境：Docker Engine与Compose v2，Python3.10+；源码开发另需JDK21、Maven3.9+、Node.js24.19.0+、npm。镜像为Temurin21、Node24.19.0、MySQL8.4、Nginx1.29。首次构建需要连接官方镜像、Maven Central和npm。

## 安装、初始化与运行

```bash
python3 scripts/init-env.py
docker compose config --quiet
docker compose up --build -d --wait
```

默认页面：[http://127.0.0.1:8134/](http://127.0.0.1:8134/)。健康检查：[http://127.0.0.1:8134/actuator/health](http://127.0.0.1:8134/actuator/health)。默认只绑定本机；数据库和后端不映射宿主机端口。

首次账号由`ADMIN_USERNAME`指定，默认`admin`。脚本在权限0600的本机`.env`生成独立随机密码，请自行查看`ADMIN_PASSWORD`。**没有固定共享密码**，脚本拒绝覆盖已有配置。更改环境变量不会重置已有账号。

空数据库首次执行`backend/src/main/resources/db/migration/V1__identity.sql`、`V2__apiary_workflows.sql`，初始化岗位、菜单、物种字典、系统参数和管理员，业务表为空。增加独立管护、复核及接收账号后建立蜂场和蜂群。`scripts/smoke.py`可在隔离空测试库创建明确TEST记录，不能用于生产数据。

端口冲突时：

```bash
WEB_PORT=18134 docker compose up --build -d --wait
```

本地开发须准备独立MySQL8.4，按`.env.example`注入`DATABASE_URL`、`DATABASE_USER`、`DATABASE_PASSWORD`和`ADMIN_PASSWORD`（Maven不自动加载`.env`），在`backend`运行`mvn spring-boot:run`，在`frontend`运行`npm ci`、`npm run dev`。开发代理默认本机8080后端；并行项目须使用独立端口与代理配置。生产前端始终使用同源`/api`。

| 环境变量 | 说明 |
| --- | --- |
| `MYSQL_ROOT_PASSWORD` | 内置MySQL空卷初始化强密码 |
| `DATABASE_PASSWORD` | 应用数据库强密码，必填 |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 空身份库初始化，密码12–72 UTF-8字节，含大小写和数字 |
| `WEB_PORT` / `BIND_ADDRESS` | 默认8134／127.0.0.1 |
| `COOKIE_SECURE` | 本机HTTP为false，正式HTTPS设true |
| `DATABASE_URL` / `DATABASE_USER` | 可选外部MySQL，须独立凭据及可信TLS |

数据库`zhuatech_apiaryflow`，`mysql-data`卷持久化；结构、索引、外键全部由版本迁移建立。升级前私有备份，在另一个全新卷恢复并核验，再更新源码；不得改写已执行迁移或用删卷代替升级。[部署与恢复](docs/deployment.md)。

## 测试方式

```bash
mvn -B -f backend/pom.xml spotless:check clean verify
cd frontend
npm ci
npm run format:check
npm run lint
npm test
npm run build
# 回到项目根目录
python3 -m venv .venv
.venv/bin/python -m pip install -r scripts/requirements-quality.txt
.venv/bin/python -m black --check scripts
.venv/bin/python scripts/release-check.py
docker compose config --quiet
git diff --check
```

后端测试覆盖输入边界、完整复核／修订／交接、历史位置、容量预留、指定人和团队隔离、CSRF、即时停用及乐观版本。H2集成测试还需配合**实际全新MySQL卷**验收。Docker Maven构建执行完整测试，不跳过。隔离测试用`python3 scripts/smoke.py --help`查看`seed`、`capture`、`compare`模式，状态文件含私有测试密码不可入库。实际检查结果见[验收说明](docs/verification.md)。

## 安全、部署与故障

部署由获授权人员配置HTTPS、可信代理、`COOKIE_SECURE=true`、安全网络及私有备份。会话Cookie为HttpOnly／SameSite Strict，BCrypt12轮、CSRF、实时身份／权限、有界输入和固定查询共同保护接口。会话在内存中，重启重新登录，不提供分布式身份。CSV中和文本公式前缀，文件名来自服务端标识，不接受文件系统路径。[安全说明](docs/security.md)。

- 启动失败：检查`docker compose logs backend`、必填配置、MySQL健康和迁移日志；不要跳过失败测试。
- 巡检无法创建：核对本人管护、启用账号、蜂群状态和转场预留。
- 不能核实：只能指定独立复核人办理，核对其团队与当前权限。
- 转场容量不足：在场和已发布接收预留均占容量；提前释放无效草稿不会改变发布预留，须取消对应发布交接。
- 不能接收：先由指定移出人确认移出，再由指定接收人确认实际接收。
- 日期或版本冲突：核对当前记录后再操作，网络中断时结果未知，不自动重放。
- 要改已核实观察：追加修订，不修改原事实；修订保持原观察日期及蜂场上下文。

[操作手册](docs/operations.md) · [业务边界](docs/business.md) · [接口](docs/api.md) · [架构](docs/architecture.md)

## 授权、贡献与联系知华科技

自有源码采用[LICENSE](LICENSE)，仅限个人学习、技术研究和非商业交流，未经上海如静知华信息科技有限公司书面授权不得商用。不是MIT、Apache等免费商用协议，不是OSI标准开源许可证。第三方组件保持原许可，见[NOTICE](NOTICE)。按现状提供，不保证观察真实性、养殖结果、食品安全或生产适用性。

本项目由知华科技（上海如静知华信息科技有限公司）提供公开源码学习版本，主要用于个人学习、技术研究与非商业交流。未经书面授权不得商用。企业信息化建设、中小企业数字化转型、中小企业 AI 转型、私有化部署、软件外包、软件项目外包、软件实施、FDE 外包、OPC 技术支持及深度定制开发，请访问知华科技官网 <https://www.zhuatech.cn/>，或添加微信 zhuatech、zhuatech2 咨询。

商业授权或深度定制开发请联系知华科技。问题和贡献请提交脱敏Issue／PR，附复现、版本和测试；不提交客户地点、业务资料或凭据。安全漏洞通过私密联系方式反馈，不在公开Issue披露凭据或可利用细节。

| 微信 zhuatech | 微信 zhuatech2 |
| :---: | :---: |
| <img src="docs/images/wechat-zhuatech.png" width="200" alt="知华科技微信 zhuatech"> | <img src="docs/images/wechat-zhuatech2.png" width="200" alt="知华科技微信 zhuatech2"> |
