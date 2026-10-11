// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.apiaryflow;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 首次初始化身份与目录，蜂群和巡检事实保持为空。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class Bootstrap implements ApplicationRunner {
  static final List<String> CODES =
      List.of(
          "apiaries",
          "manage",
          "colonies",
          "inspect",
          "review",
          "transfers",
          "plan",
          "handover",
          "export",
          "reports",
          "users",
          "roles",
          "settings",
          "audit");
  final Store db;
  final BCryptPasswordEncoder encoder;
  final String username, password;

  /** 空库配置不打印凭据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Bootstrap(
      Store db,
      BCryptPasswordEncoder encoder,
      @Value("${apiaryflow.admin-username}") String username,
      @Value("${apiaryflow.admin-password}") String password) {
    this.db = db;
    this.encoder = encoder;
    this.username = username;
    this.password = password;
  }

  /** 幂等建立岗位、菜单、物种字典及容量参数。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!db.all(Account.class).isEmpty()) return;
    AdminService.validatePassword(password);
    Rules.check(username.matches("[a-zA-Z0-9_.-]{3,60}"), "INVALID_INPUT");
    for (String c : CODES) {
      var p = new Permission();
      p.code = c;
      p.name = c;
      db.save(p);
    }
    var dept = new Department();
    dept.name = "主团队 / Main team";
    dept.zone = "Asia/Shanghai";
    db.save(dept);
    var root = role("管理员 / Administrator", "ALL", CODES);
    role(
        "蜂场协调 / Apiary coordinator",
        "DEPARTMENT",
        List.of(
            "apiaries", "manage", "colonies", "transfers", "plan", "export", "reports", "audit"));
    role(
        "蜂群管护 / Colony keeper",
        "ASSIGNED",
        List.of("apiaries", "colonies", "inspect", "transfers", "handover", "export", "reports"));
    role(
        "独立复核 / Reviewer",
        "ASSIGNED",
        List.of("apiaries", "colonies", "review", "export", "reports"));
    role(
        "团队查阅 / Reader",
        "DEPARTMENT",
        List.of("apiaries", "colonies", "transfers", "export", "reports"));
    var a = new Account();
    a.username = username.toLowerCase(Locale.ROOT);
    a.displayName = "管理员 / Administrator";
    a.passwordHash = encoder.encode(password);
    a.roleId = root.id;
    a.departmentId = dept.id;
    db.save(a);
    String[][] menus = {
      {"colonies", "蜂群台账", "Colonies", "colonies"},
      {"inspections", "巡检与复核", "Inspections", "colonies"},
      {"transfers", "转场交接", "Transfers", "transfers"},
      {"apiaries", "蜂场目录", "Apiaries", "apiaries"},
      {"reports", "业务统计", "Reports", "reports"},
      {"users", "登录账号", "Accounts", "users"},
      {"roles", "角色与权限", "Roles", "roles"},
      {"settings", "团队与设置", "Settings", "settings"},
      {"audit", "审计记录", "Audit", "audit"}
    };
    for (int i = 0; i < menus.length; i++) {
      var m = new NavMenu();
      m.code = menus[i][0];
      m.name = menus[i][1];
      m.nameEn = menus[i][2];
      m.permissionCode = menus[i][3];
      m.position = i;
      m.enabled = true;
      db.save(m);
    }
    for (var v :
        new String[][] {
          {"MELLIFERA", "西方蜜蜂", "Apis mellifera"},
          {"CERANA", "东方蜜蜂", "Apis cerana"},
          {"UNKNOWN", "未确定", "Unspecified"}
        }) {
      var x = new DictionaryEntry();
      x.type = "SPECIES";
      x.code = v[0];
      x.name = v[1];
      x.nameEn = v[2];
      db.save(x);
    }
    for (String c : List.of("max_colonies", "max_transfers", "max_inspections")) {
      var s = new SystemSetting();
      s.code = c;
      s.value = "500";
      db.save(s);
    }
  }

  private AccessRole role(String name, String scope, List<String> codes) {
    var r = new AccessRole();
    r.name = name;
    r.scope = scope;
    r.permissions.addAll(codes);
    return db.save(r);
  }
}
