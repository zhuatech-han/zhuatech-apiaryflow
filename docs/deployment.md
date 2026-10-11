# ApiaryFlow 部署、升级与恢复 / Deployment

知华科技（上海如静知华信息科技有限公司）· https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2。

## 本机初始化 / Local initialization

```bash
python3 scripts/init-env.py
docker compose -p apiaryflow-local config --quiet
docker compose -p apiaryflow-local up --build -d --wait
```

脚本生成0600独立.env，默认127.0.0.1:8134，WEB_PORT可覆盖。不要公开普通`docker compose config`输出，可能含密码。首次空卷执行FlywayV1/V2，业务为空，重启不重设密码。生产必须先获商业授权并另行配置HTTPS和可信代理；COOKIE_SECURE=true，独立数据库凭据及网络访问控制。

Private unique configuration, localhost binding and versioned empty-volume setup. Restart does not reset passwords. Authorized production deployments require HTTPS/trusted proxies, secure cookies and credentials; local validation does not certify production suitability.

## 私有一致性备份与全新恢复 / Backup and new restore

```bash
python3 scripts/backup.py --project apiaryflow-local --output /private/path/apiaryflow-backup.zip
python3 scripts/restore.py /private/path/apiaryflow-backup.zip --project apiaryflow-restore --env-file /private/path/restore.env
```

项目名须与原启动项目一致。备份暂停本项目后端写入，通过MySQL一致性导出后恢复后端；归档包含散列密码和业务事实，不得上传Git。恢复验证归档成员、大小和SHA，只恢复到不存在的独立项目/卷/网络，拒绝覆盖现有数据库，配置使用新密码和端口。只支持Compose内置MySQL；外部DATABASE_URL不可混用。SQL不是沙箱，仅恢复可信备份。

Use the actual Compose project name. Backup pauses this backend's writes for a consistent MySQL dump and restarts it. Archives are private. Restore validates members, size and SHA and refuses an existing project/volume/network. Bundled MySQL only; trusted dumps only.

升级前先在全新隔离实例恢复，核对目录、账号权限、蜂群位置/蜂王/谱系、巡检原始和修订、交接清单/实际日期、事件、非登录审计及CSV。恢复后的ADMIN_PASSWORD不会覆盖原账号。不得改写已执行迁移，不用删卷代替升级。

Verify identities, locations, queen/split facts, original/corrected inspections, transfer manifests/dates, events, non-login audit and exports in a new restore before upgrading. Bootstrap configuration does not replace restored passwords.

## 停止与清理 / Stop and cleanup

一般停止：`docker compose -p apiaryflow-local down`，保留卷。仅独立可丢弃测试项目使用`down -v`；会删除该测试卷，不能用于没有备份的业务实例。不停止其他项目，也不全局清理容器、卷或缓存。

Stop with `down` to retain data. `down -v` is only for isolated disposable test resources. Never prune unrelated projects.
