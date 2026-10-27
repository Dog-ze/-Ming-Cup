create table user_point_account (
    user_id bigint not null primary key comment '用户 ID',
    balance int not null default 0 comment '当前可用积分',
    created_at datetime not null default current_timestamp comment '创建时间',
    updated_at datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    constraint fk_point_account_user foreign key (user_id) references user (id) on delete cascade,
    constraint chk_point_account_balance check (balance >= 0)
) comment '用户积分账户表';

create table user_point_transaction (
    id bigint auto_increment primary key comment '积分流水 ID',
    user_id bigint not null comment '所属用户 ID',
    type varchar(32) not null comment '积分类型',
    change_amount int not null comment '积分变化',
    balance_after int not null comment '变化后的余额',
    business_key varchar(100) not null comment '业务幂等键',
    related_record_id bigint null comment '关联业务记录 ID',
    status varchar(16) not null default 'SUCCESS' comment 'PENDING、SUCCESS、FAILED、REFUNDED',
    description varchar(200) not null comment '说明',
    admin_id bigint null comment '管理员 ID',
    created_at datetime not null default current_timestamp comment '创建时间',
    constraint fk_point_transaction_user foreign key (user_id) references user (id) on delete cascade,
    constraint fk_point_transaction_admin foreign key (admin_id) references admin_user (id) on delete set null,
    constraint uk_point_transaction_business unique (business_key)
) comment '用户积分流水表';

create index idx_point_transaction_user_created on user_point_transaction (user_id, created_at desc, id desc);
