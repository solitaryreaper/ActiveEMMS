# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table item_data (
  id                        bigint auto_increment not null,
  item_id                   varchar(255),
  attribute                 varchar(255),
  value                     varchar(255),
  job_id                    bigint,
  constraint pk_item_data primary key (id))
;

create table itempair_gold_data (
  id                        bigint auto_increment not null,
  item1id                   varchar(255),
  item2id                   varchar(255),
  match_status              integer,
  is_labelled_in_train_phase tinyint(1) default 0,
  job_id                    bigint,
  constraint ck_itempair_gold_data_match_status check (match_status in (0,1,2)),
  constraint pk_itempair_gold_data primary key (id))
;

create table job (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  description               varchar(255),
  dataset_name              varchar(255),
  project_id                bigint,
  status                    integer,
  constraint ck_job_status check (status in (0,1,2,3)),
  constraint pk_job primary key (id))
;

create table project (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  description               varchar(255),
  constraint pk_project primary key (id))
;

create table rule (
  id                        bigint auto_increment not null,
  name                      varchar(255),
  precision_metric          double,
  coverage_metric           double,
  job_id                    bigint,
  constraint pk_rule primary key (id))
;

alter table item_data add constraint fk_item_data_job_1 foreign key (job_id) references job (id) on delete restrict on update restrict;
create index ix_item_data_job_1 on item_data (job_id);
alter table itempair_gold_data add constraint fk_itempair_gold_data_job_2 foreign key (job_id) references job (id) on delete restrict on update restrict;
create index ix_itempair_gold_data_job_2 on itempair_gold_data (job_id);
alter table job add constraint fk_job_project_3 foreign key (project_id) references project (id) on delete restrict on update restrict;
create index ix_job_project_3 on job (project_id);
alter table rule add constraint fk_rule_job_4 foreign key (job_id) references job (id) on delete restrict on update restrict;
create index ix_rule_job_4 on rule (job_id);



# --- !Downs

SET FOREIGN_KEY_CHECKS=0;

drop table item_data;

drop table itempair_gold_data;

drop table job;

drop table project;

drop table rule;

SET FOREIGN_KEY_CHECKS=1;

