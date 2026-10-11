// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.apiaryflow;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** 蜂群稳定身份、当前蜂场、指定管护人与分群关系。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "colony")
public class Colony {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "code", nullable = false, length = 30)
  public String code;

  @Column(name = "name", nullable = false, length = 120)
  public String name;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "apiary_id", nullable = false)
  public Long apiaryId;

  @Column(name = "keeper_id", nullable = false)
  public Long keeperId;

  @Column(name = "created_by", nullable = false)
  public Long createdBy;

  @Column(name = "category", nullable = false, length = 60)
  public String category;

  @Column(name = "source", nullable = false, length = 300)
  public String source;

  @Column(name = "opened_on", nullable = false)
  public LocalDate openedOn;

  @Column(name = "last_on", nullable = false)
  public LocalDate lastOn;

  @Column(name = "state", nullable = false, length = 20)
  public String state;

  @Column(name = "parent_id", nullable = true)
  public Long parentId;

  @Column(name = "move_id", nullable = true)
  public Long moveId;

  @Column(name = "queen_code", nullable = false, length = 60)
  public String queenCode = "";

  @Column(name = "queen_since", nullable = true)
  public LocalDate queenSince;

  @Column(name = "version", nullable = false)
  public long version = 1;
}
