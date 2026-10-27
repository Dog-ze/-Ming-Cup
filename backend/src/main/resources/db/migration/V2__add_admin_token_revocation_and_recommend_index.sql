create table admin_token_revocation
(
    token_id   varchar(64)                            not null comment 'JWT 的唯一 jti',
    expires_at datetime                               not null comment '原 Token 的过期时间',
    revoked_at datetime default CURRENT_TIMESTAMP     not null comment '撤销时间',
    primary key (token_id),
    index idx_admin_token_revocation_expires_at (expires_at)
)
    comment '已退出的后台管理员 Token';

create index idx_tea_enabled_category
    on tea (enabled, category);
