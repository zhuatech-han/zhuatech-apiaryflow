# ApiaryFlow 安全 / Security

知华科技（上海如静知华信息科技有限公司）· https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2。

公开源码学习版，未经书面授权不得商用。商业部署须由获授权人员配置HTTPS、可信代理边界、COOKIE_SECURE、安全网络、访问控制和私有备份。默认只监听127.0.0.1前端，数据库/后端不映射宿主机端口。外部MySQL连接必须校验身份TLS与可信CA，不关闭证书校验。

Source-available non-commercial edition. Authorized commercial deployment requires HTTPS, trusted proxies, secure cookies/networks, access control and private backups. External MySQL requires verified TLS and a trusted CA.

BCrypt12轮，密码12–72UTF-8字节，含大小写及数字；随机独立初始化，真实.env忽略。会话HttpOnly/SameSiteStrict，CSRF保护写入；账号/团队启用、密码指纹及当前角色在每次业务请求重新核验。登录失败窗口有限、内存保存，不是分布式抗攻击保证。内存会话重启后重新登录。最后完整管理员受保护。

Passwords use BCrypt12 and unique initialization. Sessions/CSRF and live identity checks protect writes; no distributed identity or abuse-prevention guarantee is provided.

不上传真实蜂场地点、人员私密资料、客户数据、备份或凭据。核心实体和候选目录受团队/本人/指定范围约束，密码散列不返回。应用无附件上传路径，导出使用服务端文件名；CSV中和公式文本。对应用管理员和数据库管理员的权限须另行控制；快照摘要不抵抗有权修改数据库的人，不是不可篡改认证。

Do not publish actual site locations, private personal data, client records, backups or credentials. Application snapshot hashes are not tamper-proof against database administrators. CSV filenames and text are constrained.

发现漏洞请私下通过官网、中文商业微信或英文README的已提供联系方式反馈，附脱敏版本和复现。不在公开Issue披露凭据、可利用细节或客户资料。第三方依赖遵守NOTICE，不因自有非商业许可覆盖其许可证。无法保证没有未知漏洞。
