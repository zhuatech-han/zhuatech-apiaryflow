// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.apiaryflow;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** 冻结蜂群清单、指定移出与接收人和真实交接日期。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "colony_transfer")
public class Transfer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "code", nullable = false, length = 30)
  public String code;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "source_id", nullable = false)
  public Long sourceId;

  @Column(name = "destination_id", nullable = false)
  public Long destinationId;

  @Column(name = "created_by", nullable = false)
  public Long createdBy;

  @Column(name = "sender_id", nullable = false)
  public Long senderId;

  @Column(name = "receiver_id", nullable = false)
  public Long receiverId;

  @Column(name = "planned_on", nullable = false)
  public LocalDate plannedOn;

  @Column(name = "departed_on", nullable = true)
  public LocalDate departedOn;

  @Column(name = "received_on", nullable = true)
  public LocalDate receivedOn;

  @Column(name = "state", nullable = false, length = 20)
  public String state;

  @Column(name = "notes", nullable = false, length = 500)
  public String notes;

  @Column(name = "snapshot_json", nullable = false, columnDefinition = "longtext")
  public String snapshotJson = "";

  @Column(name = "digest", nullable = false, length = 64)
  public String digest = "";

  @Column(name = "version", nullable = false)
  public long version = 1;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "transfer_colony", joinColumns = @JoinColumn(name = "transfer_id"))
  @Column(name = "colony_id")
  public Set<Long> colonyIds = new LinkedHashSet<>();
}
