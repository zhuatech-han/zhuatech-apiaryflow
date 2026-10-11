# ApiaryFlow 接口 / API

知华科技（上海如静知华信息科技有限公司）· https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2。

同源`/api`，JSON。每次写入先GET`/auth/csrf`，使用返回header/token；登录后重新获取。POST`/auth/login`使用username/password，GET`/auth/me`返回安全身份、团队时区、当前菜单；POST`/auth/logout`退出；POST`/auth/password`自行修改密码（oldPassword/newPassword），成功后会话失效。密码和会话不写日志。

Same-origin JSON API. Obtain a session CSRF token before writes and again after login. Authentication routes never return password hashes. Updating one's password revokes the session.

| 路径 / Route | 行为 / Behavior |
| --- | --- |
| GET `/apiaries`, `/colonies`, `/inspections`, `/transfers` | 按当前权限／范围读取真实有界列表 / Scoped lists |
| GET `/options` | 同团队最小蜂场、候选岗位及分类 / Minimal form options |
| POST `/apiaries`, PUT `/apiaries/{id}` | 蜂场管理 / Apiary management |
| POST `/colonies`, PUT `/colonies/{id}` | 蜂群建档及合法维护 / Register and maintain colonies |
| GET `/colonies/{id}` | 蜂群、可见巡检、事件及可见分出的蜂群 / Colony history |
| DELETE `/colonies/{id}` | 仅从未使用误建记录 / Never-used erroneous registration only |
| POST `/colonies/{id}/split` | 新编号/name、happenedOn、note / Declared split |
| POST `/colonies/{id}/requeen` | queenCode、happenedOn、note / Queen change |
| POST `/colonies/{id}/retire` | happenedOn、note / Preserve history and retire |
| POST `/inspections`, PUT `/inspections/{id}` | 当前管护人草稿 / Assigned keeper draft |
| GET `/inspections/{id}` | record及superseded标记 / Record and supersession flag |
| DELETE `/inspections/{id}` | 仅未送审的本人误建草稿 / Own never-submitted draft |
| POST `/inspections/{id}/{action}` | submit/withdraw/cancel/verify/return |
| POST `/transfers`, PUT `/transfers/{id}` | 同团队转场草稿 / Same-team draft manifest |
| GET `/transfers/{id}` | 清单、冻结摘要及实际日期 / Manifest and factual dates |
| POST `/transfers/{id}/{action}` | release/cancel/depart/receive |
| GET或HEAD `/{inspections或transfers}/{id}/export.csv` | 同读取与export权限 / Read and export rights required |
| GET `/reports`, `/audit` | 有范围的统计和最近200审计 / Scoped reports/audit |
| GET/POST `/admin/{kind}`，PUT `/admin/{kind}/{id}` | users/roles/permissions/departments/menus/dictionaries/settings，按对应权限及ALL范围 / Admin rights and All scope |
| GET `/admin/options` | 账号管理表单的角色与团队 / Account-admin options |

业务更新、删除和动作必须提供当前`version`，账号／角色／团队管理也校验版本；其他系统目录以最后保存者为准。冲突409；新增业务由服务端产生版本。需要说明的动作`note`4–500字符，现场说明允许换行。不接受客户端创建状态、角色、审计、摘要或事件。

Business updates/deletes/actions and account/role/team updates require the current version. Other catalogues use last-save semantics. Server controls state, snapshots, events and audit. Notes are4–500characters; observations allow line breaks.

蜂场新增：code/name/departmentId/location/capacity/enabled。蜂群新增：code/name/apiaryId/keeperId/category/source/openedOn。巡检：colonyId/observedOn/coveredFrames/broodFrames/queenSeen/stores/broodPattern/observations/reviewerId；修订另传supersedesId。转场：code/sourceId/destinationId/senderId/receiverId/plannedOn/notes/colonyIds数组1–50个不重复蜂群。所有单位、分类、日期、身份和枚举均由服务端验证。

Lists are bounded at10000 and business capacity parameters are lower. UI search/filter/sort/pagination operate over authorized lists, not a claim of server-side paging. Standard error responses contain a code only:401unauthenticated,403permission/scope,400invalid input,409state/version/constraint,413resource limit. An unknown submission result after a network failure must be checked before retrying.
