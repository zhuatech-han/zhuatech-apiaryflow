// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.apiaryflow;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** 指定管护人记录、独立复核及追加修订，核实历史不覆盖。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "inspection")
public class Inspection {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "colony_id", nullable = false)
  public Long colonyId;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "inspector_id", nullable = false)
  public Long inspectorId;

  @Column(name = "reviewer_id", nullable = false)
  public Long reviewerId;

  @Column(name = "supersedes_id", nullable = true)
  public Long supersedesId;

  @Column(name = "observed_on", nullable = false)
  public LocalDate observedOn;

  @Column(name = "covered_frames", nullable = false)
  public int coveredFrames;

  @Column(name = "brood_frames", nullable = false)
  public int broodFrames;

  @Column(name = "queen_seen", nullable = false)
  public boolean queenSeen;

  @Column(name = "stores", nullable = false, length = 20)
  public String stores;

  @Column(name = "brood_pattern", nullable = false, length = 20)
  public String broodPattern;

  @Column(name = "observations", nullable = false, length = 2000)
  public String observations;

  @Column(name = "state", nullable = false, length = 20)
  public String state;

  @Column(name = "decision_note", nullable = false, length = 500)
  public String decisionNote = "";

  @Column(name = "snapshot_json", nullable = false, columnDefinition = "longtext")
  public String snapshotJson = "";

  @Column(name = "digest", nullable = false, length = 64)
  public String digest = "";

  @Column(name = "version", nullable = false)
  public long version = 1;
}
