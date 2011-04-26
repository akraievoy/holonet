SET COMPRESS_LOB DEFLATE;
SET MAX_LENGTH_INPLACE_LOB 512;

drop table "hn_ctx" if exists;
drop table "hn_run" if exists;
drop table "hn_conf" if exists;
drop table "hn_par" if exists;
drop table "hn_exp" if exists;
drop table "hn_info" if exists;

create table "hn_info" (
    "path"        varchar(64) not null primary key,
    "val"         varchar(256) not null default ''
);

create table "hn_exp" (
    "uid"         bigint auto_increment(101,11) primary key,
    "id"          varchar(256) not null,
    "path"        varchar(256) not null,
    "depends"     varchar(256) not null,
    "desc"        varchar(512) not null,
    "millis"      bigint not null,
    "springXml"   blob not null
);

drop index if exists "hn_exp_query_index_millis";
create index "hn_exp_query_index_millis" on "hn_exp"("millis");

create table "hn_conf" (
    "uid"         bigint auto_increment(103,13) primary key,
    "exp_uid"     bigint not null,
    "name"        varchar(256) not null,
    "desc"        varchar(512) not null
);

alter table "hn_conf" add constraint "conf_references_exp"
  foreign key ("exp_uid") references "hn_exp" on delete cascade;

drop index if exists "hn_conf_query_index_name";
create index "hn_conf_query_index_name" on "hn_conf"("name");

create table "hn_par" (
    "uid"               bigint auto_increment(107,17) primary key,
    "conf_uid"          bigint not null,
    "name"              varchar(256) not null,
    "value"             varchar(512) not null,
    "desc"              varchar(512) not null default '',
    "strategy"          varchar(32) not null default 'iterate',
    "chainStrategy"     varchar(32) not null default 'iterate'
);

alter table "hn_par" add constraint "par_references_conf"
  foreign key ("conf_uid") references "hn_conf" on delete cascade;

drop index if exists "hn_par_query_index_name";
create index "hn_par_query_index_name" on "hn_par"("name");

create table "hn_run" (
    "uid"          bigint auto_increment(109,19) primary key,
    "conf_uid"     bigint not null,
    "millis"       bigint not null,
    "chain"        varchar(256) not null default '',
    "psetCount"    bigint not null default 0,
    "psetComplete" bigint not null default 0
);

alter table "hn_run" add constraint "run_references_conf"
  foreign key ("conf_uid") references "hn_conf" on delete cascade;

drop index if exists "hn_run_query_index_millis";
create index "hn_run_query_index_millis" on "hn_run"("millis");

create table "hn_ctx" (
    "uid"       bigint auto_increment(113,23) primary key,
    "run_uid"   bigint not null,
    "psetI"     bigint not null,
    "millis"    bigint not null,
    "path"      varchar(256) not null,
    "type"      varchar(256) not null,
    "val"       varchar(256),
    "content"   blob
);

alter table "hn_ctx" add constraint "ctx_references_run"
  foreign key ("run_uid") references "hn_run" on delete cascade;

drop index if exists "hn_ctx_query_index_psetI";
create index "hn_ctx_query_index_psetI" on "hn_ctx"("psetI");
drop index if exists "hn_ctx_query_index_path";
create index "hn_ctx_query_index_path" on "hn_ctx"("path");
drop index if exists "hn_ctx_query_index_run_psetI";
create index "hn_ctx_query_index_run_psetI" on "hn_ctx"("run_uid", "psetI");
drop index if exists "hn_ctx_query_index_run_path";
create index "hn_ctx_query_index_run_path" on "hn_ctx"("run_uid", "path");
drop index if exists "hn_ctx_query_index_millis";
create index "hn_ctx_query_index_millis" on "hn_ctx"("millis");

insert into "hn_info" ("path", "val") values ('schema.version', '1');
