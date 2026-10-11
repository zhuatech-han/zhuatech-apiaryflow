// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.apiaryflow;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** 追加蜂群建档、分群、换王、移出接收与退出事实。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "colony_event")
public class ColonyEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "colony_id", nullable = false)
  public Long colonyId;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "actor_id", nullable = false)
  public Long actorId;

  @Column(name = "action", nullable = false, length = 30)
  public String action;

  @Column(name = "happened_on", nullable = false)
  public LocalDate happenedOn;

  @Column(name = "snapshot_json", nullable = false, columnDefinition = "longtext")
  public String snapshotJson = "";

  @Column(name = "notes", nullable = false, length = 500)
  public String notes;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
