-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
-- 仅供非商业学习；蜂场、蜂群与记录首次保持为空。

create table apiary_site (
 id bigint auto_increment primary key,
 code varchar(30) not null,
 name varchar(120) not null,
 department_id bigint not null,
 location varchar(300) not null,
 capacity int not null,
 enabled boolean not null,
 version bigint not null,
 unique(code),
 foreign key(department_id) references department(id)
);

create table colony (
 id bigint auto_increment primary key,
 code varchar(30) not null,
 name varchar(120) not null,
 department_id bigint not null,
 apiary_id bigint not null,
 keeper_id bigint not null,
 created_by bigint not null,
 category varchar(60) not null,
 source varchar(300) not null,
 opened_on date not null,
 last_on date not null,
 state varchar(20) not null,
 parent_id bigint,
 move_id bigint,
 queen_code varchar(60) not null,
 queen_since date,
 version bigint not null,
 unique(code),
 foreign key(department_id) references department(id),
 foreign key(apiary_id) references apiary_site(id),
 foreign key(keeper_id) references account(id),
 foreign key(created_by) references account(id),
 foreign key(parent_id) references colony(id)
);

create table inspection (
 id bigint auto_increment primary key,
 colony_id bigint not null,
 department_id bigint not null,
 inspector_id bigint not null,
 reviewer_id bigint not null,
 supersedes_id bigint,
 observed_on date not null,
 covered_frames int not null,
 brood_frames int not null,
 queen_seen boolean not null,
 stores varchar(20) not null,
 brood_pattern varchar(20) not null,
 observations varchar(2000) not null,
 state varchar(20) not null,
 decision_note varchar(500) not null,
 snapshot_json longtext not null,
 digest varchar(64) not null,
 version bigint not null,
 foreign key(department_id) references department(id),
 foreign key(colony_id) references colony(id),
 foreign key(inspector_id) references account(id),
 foreign key(reviewer_id) references account(id),
 foreign key(supersedes_id) references inspection(id)
);

create table colony_transfer (
 id bigint auto_increment primary key,
 code varchar(30) not null,
 department_id bigint not null,
 source_id bigint not null,
 destination_id bigint not null,
 created_by bigint not null,
 sender_id bigint not null,
 receiver_id bigint not null,
 planned_on date not null,
 departed_on date,
 received_on date,
 state varchar(20) not null,
 notes varchar(500) not null,
 snapshot_json longtext not null,
 digest varchar(64) not null,
 version bigint not null,
 unique(code),
 foreign key(department_id) references department(id),
 foreign key(source_id) references apiary_site(id),
 foreign key(destination_id) references apiary_site(id),
 foreign key(sender_id) references account(id),
 foreign key(receiver_id) references account(id),
 foreign key(created_by) references account(id)
);

create table colony_event (
 id bigint auto_increment primary key,
 colony_id bigint not null,
 department_id bigint not null,
 actor_id bigint not null,
 action varchar(30) not null,
 happened_on date not null,
 snapshot_json longtext not null,
 notes varchar(500) not null,
 created_at timestamp(6) not null,
 foreign key(department_id) references department(id),
 foreign key(colony_id) references colony(id),
 foreign key(actor_id) references account(id)
);

create table transfer_colony (transfer_id bigint not null, colony_id bigint not null, primary key(transfer_id,colony_id), foreign key(transfer_id) references colony_transfer(id), foreign key(colony_id) references colony(id));

alter table colony add constraint colony_move_fk foreign key(move_id) references colony_transfer(id);

create index colony_scope_idx on colony(department_id,keeper_id,apiary_id,state);

create index inspection_colony_idx on inspection(colony_id,state,observed_on);

create index transfer_scope_idx on colony_transfer(department_id,sender_id,receiver_id,state);

create index event_colony_idx on colony_event(colony_id,happened_on);
