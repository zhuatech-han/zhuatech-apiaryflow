// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.apiaryflow;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** 固定资源接口，不能由客户端指定实体或绕过状态服务。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api")
public class Api {
  final ApiaryService service;
  final AdminService admin;

  /** 连接真实业务与目录服务。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Api(ApiaryService service, AdminService admin) {
    this.service = service;
    this.admin = admin;
  }

  /** 授权查询、统计及最小表单目录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/{kind}")
  public Object read(@PathVariable String kind) {
    return service.read(kind);
  }

  /** 新增蜂场、蜂群、巡检或转场草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/{kind}")
  public Object create(@PathVariable String kind, @RequestBody Map<String, Object> b) {
    return save(kind, null, b);
  }

  /** 乐观版本防止覆盖别人提交的记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/{kind}/{id}")
  public Object update(
      @PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return save(kind, id, b);
  }

  private Object save(String kind, Long id, Map<String, Object> b) {
    return switch (kind) {
      case "apiaries" -> service.saveSite(id, b);
      case "colonies" -> service.saveColony(id, b);
      case "inspections" -> service.saveInspection(id, b);
      case "transfers" -> service.saveTransfer(id, b);
      default -> throw new Problem(404, "NOT_FOUND");
    };
  }

  /** 读取历史和冻结快照。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/{kind}/{id}")
  public Object detail(@PathVariable String kind, @PathVariable Long id) {
    return service.detail(kind, id);
  }

  /** 删除仅限从未使用的误建记录；历史事实由服务端阻止删除。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/{kind}/{id}")
  public Object delete(
      @PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return switch (kind) {
      case "colonies" -> service.deleteColony(id, b);
      case "inspections" -> service.deleteInspection(id, b);
      default -> throw new Problem(404, "NOT_FOUND");
    };
  }

  /** 状态办理和分群、换王均校验岗位、指定人、范围与版本。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/{kind}/{id}/{action}")
  public Object action(
      @PathVariable String kind,
      @PathVariable Long id,
      @PathVariable String action,
      @RequestBody Map<String, Object> b) {
    return switch (kind) {
      case "colonies" -> service.colonyAction(id, action, b);
      case "inspections" -> service.inspectionAction(id, action, b);
      case "transfers" -> service.transferAction(id, action, b);
      default -> throw new Problem(404, "NOT_FOUND");
    };
  }

  /** CSV和HEAD共享授权检查，文件名不接受客户端路径。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/{kind}/{id}/export.csv")
  public ResponseEntity<String> export(@PathVariable String kind, @PathVariable Long id) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .header("Cache-Control", "no-store")
        .header(
            "Content-Disposition", "attachment; filename=apiaryflow-" + kind + "-" + id + ".csv")
        .body(service.export(kind, id));
  }

  /** 后台目录按具体权限与ALL范围限制。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/{kind}")
  public Object directory(@PathVariable String kind) {
    return kind.equals("options") ? admin.options() : admin.read(kind);
  }

  /** 新增管理员目录，保持最后完整管理员。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/admin/{kind}")
  public Object add(@PathVariable String kind, @RequestBody Map<String, Object> b) {
    return admin.save(kind, null, b);
  }

  /** 修改岗位、账号与设置，不泄露密码散列。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/admin/{kind}/{id}")
  public Object change(
      @PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return admin.save(kind, id, b);
  }
}
