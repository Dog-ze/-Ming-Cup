create table action_log
(
    id         bigint auto_increment comment '操作日志主键 ID'
        primary key,
    user_id    bigint                             null comment '执行操作的用户 ID；未登录操作时允许为空',
    action     varchar(40)                        not null comment '操作名称，如 view_recommend、create_record',
    detail     json                               null comment '操作附加信息 JSON，如页面名称、城市',
    created_at datetime default CURRENT_TIMESTAMP not null comment '操作发生时间'
)
    comment '用户操作埋点日志表';

create index idx_log_user_time
    on action_log (user_id asc, created_at desc);

create table admin_operation_log
(
    id         bigint auto_increment comment '操作日志主键 ID'
        primary key,
    admin_id   bigint                             not null comment '操作管理员 ID',
    module     varchar(32)                        not null comment '模块：user、tea、pairing_rule、store 等',
    action     varchar(32)                        not null comment '动作：create、update、enable、disable、promotion 等',
    target_id  bigint                             null comment '被操作对象 ID',
    detail     json                               null comment '操作前后必要字段摘要，不能记录密码和完整定位',
    created_at datetime default CURRENT_TIMESTAMP not null comment '操作时间'
)
    comment '后台管理员操作审计日志表';

create index idx_admin_operation_time
    on admin_operation_log (admin_id, created_at);

create index idx_operation_log_module_action_time
    on admin_operation_log (module, action, created_at, id);

create index idx_operation_target
    on admin_operation_log (module, target_id);

create table admin_user
(
    id            bigint auto_increment comment '管理员主键 ID'
        primary key,
    username      varchar(32)                        not null comment '管理员登录账号',
    password_hash varchar(100)                       not null comment 'BCrypt 加密后的管理员密码',
    display_name  varchar(32)                        not null comment '后台显示名称',
    status        tinyint  default 1                 not null comment '账号状态：0 停用，1 启用',
    created_at    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '最后修改时间',
    constraint uk_admin_user_username
        unique (username)
)
    comment '后台管理员账号表';

create table pairing_rule
(
    id           bigint auto_increment comment '本地茶食搭配规则主键 ID'
        primary key,
    tea_category varchar(16)                        not null comment '适用茶类，如绿茶、红茶',
    foods        json                               not null comment '推荐搭配食品名称数组',
    advice       varchar(300)                       not null comment '给用户展示的日常搭配建议',
    updated_at   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '最后更新时间',
    constraint uk_pairing_rule_tea_category
        unique (tea_category)
)
    comment '本地茶食搭配规则表';

create table pairing_store
(
    id                 bigint auto_increment comment '店铺编号'
        primary key,
    store_name         varchar(100)                       not null comment '店铺名称',
    cover_image        varchar(255)                       null comment '店铺封面图片访问地址',
    address            varchar(255)                       not null comment '店铺详细地址',
    longitude          decimal(10, 7)                     null comment '店铺经度，例如 120.5853150',
    latitude           decimal(10, 7)                     null comment '店铺纬度，例如 31.2988860',
    phone              varchar(20)                        null comment '店铺联系电话',
    tea_categories     json                               not null comment '适配茶类，例如 ["绿茶", "白茶"]',
    food_tags          json                               not null comment '推荐食物标签，例如 ["清淡糕点", "水果"]',
    description        varchar(500)                       null comment '店铺推荐说明',
    priority           int      default 0                 not null comment '排序优先级，数字越大越靠前',
    status             tinyint  default 1                 not null comment '展示状态：0 下架，1 上架',
    created_at         datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updated_at         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '最后修改时间',
    promotion_score    int      default 0                 not null comment '推广排序分数，仅后台使用，建议 0 到 30',
    promotion_enabled  tinyint  default 0                 not null comment '是否启用推广：0 否，1 是',
    promotion_start_at datetime                           null comment '推广开始时间，为空表示立即生效',
    promotion_end_at   datetime                           null comment '推广结束时间，为空表示不设结束时间'
)
    comment '一盏茗茶食搭配店铺表' collate = utf8mb4_unicode_ci;

create index idx_pairing_store_location
    on pairing_store (longitude, latitude);

create index idx_pairing_store_promotion
    on pairing_store (promotion_enabled, promotion_start_at, promotion_end_at);

create index idx_pairing_store_status_priority
    on pairing_store (status, priority);

create table tea
(
    id                bigint auto_increment comment '茶叶主键 ID'
        primary key,
    name              varchar(64)                           not null comment '茶叶名称，如西湖龙井',
    category          varchar(16)                           not null comment '茶叶类别：绿茶、白茶、黄茶、青茶、红茶、黑茶或花草',
    nature            varchar(8)                            not null comment '茶性：寒、凉、平或温',
    caffeine          varchar(8)                            not null comment '咖啡因等级：低、中或高',
    flavor_tags       json                                  not null comment '口感标签数组，如["清淡鲜爽","栗香"]',
    effect_tags       json                                  not null comment '日常饮用场景标签数组，如["解腻","提神"]，不代表医疗功效',
    suitable_strength varchar(10)                           not null comment '推荐冲泡浓度：清淡、适中或浓厚',
    brew_temp         int                                   null comment '建议冲泡水温，单位为摄氏度',
    brew_time         int                                   null comment '建议冲泡时间，单位为秒',
    source            varchar(16) default 'local'           not null comment '数据来源：local 或 teafyi',
    enabled           tinyint     default 1                 not null comment '是否在 App 中启用：1 启用，0 停用',
    created_at        datetime    default CURRENT_TIMESTAMP not null comment '茶叶资料创建时间',
    status            tinyint     default 1                 not null comment '1上架，0下架',
    image_url         varchar(500)                          null comment '图片地址',
    updated_at        datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint name
        unique (name)
)
    comment '茶叶知识库表';

create index idx_tea_status_category
    on tea (status, category);

create table user
(
    id            bigint auto_increment comment '用户主键 ID'
        primary key,
    username      varchar(32)                           not null comment '登录用户名，系统内唯一',
    password_hash varchar(100)                          not null comment '经 BCrypt 加密后的密码，不保存明文',
    nickname      varchar(32)                           null,
    city          varchar(64) default '北京市'          null comment '用户所在城市，默认为北京市',
    longitude     decimal(10, 6)                        null comment '用户所在城市或定位的经度',
    latitude      decimal(10, 6)                        null comment '用户所在城市或定位的纬度',
    created_at    datetime    default CURRENT_TIMESTAMP not null comment '用户注册时间',
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '用户资料最后修改时间',
    phone         varchar(20)                           null comment '用户手机号，登录身份，唯一',
    email         varchar(128)                          null comment '用户邮箱，登录身份，唯一',
    avatar        varchar(255)                          null comment '用户头像访问地址',
    status        tinyint     default 1                 not null comment '账号状态：0 停用，1 启用',
    constraint uk_user_email
        unique (email),
    constraint uk_user_phone
        unique (phone)
)
    comment '用户基本信息表';

create table questionnaire
(
    id                  bigint auto_increment comment '问卷主键 ID'
        primary key,
    user_id             bigint                             not null comment '所属用户 ID；一名用户只保留一份最新问卷',
    purpose             varchar(20)                        not null comment '喝茶目的：解腻、提神、暖胃、降火、放松或随便',
    constitution_type   varchar(20)                        not null comment '用户自述状态：怕冷、怕热或都还好',
    insomnia            varchar(20)                        not null comment '是否容易失眠：容易、不容易或不确定',
    flavor_preference   varchar(30)                        not null comment '口感偏好，如清淡鲜爽、花香果香',
    strength_preference varchar(10)                        not null comment '浓度偏好：清淡、适中或浓厚',
    updated_at          datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '问卷最后提交时间',
    constraint user_id
        unique (user_id),
    constraint fk_questionnaire_user
        foreign key (user_id) references user (id)
            on delete cascade
)
    comment '用户喝茶偏好问卷表';

create index idx_questionnaire_updated_user
    on questionnaire (updated_at, user_id);

create table tea_record
(
    id                 bigint auto_increment comment '品茶记录主键 ID'
        primary key,
    user_id            bigint                                not null comment '创建该记录的用户 ID',
    tea_name           varchar(64)                           not null comment '本次饮用的茶名',
    category           varchar(16)                           not null comment '本次饮用的茶类',
    brand              varchar(64)                           null comment '茶叶品牌；不知道或散装茶可为空',
    acquisition_method varchar(16) default '其他'            not null comment '茶叶获取方式：线上购买、线下购买、朋友赠送或其他',
    price              decimal(10, 2)                        null comment '购买价格，单位为人民币元；赠送或未知价格可为空',
    brew_temp          int                                   null comment '实际冲泡水温，单位为摄氏度',
    rating             tinyint                               not null comment '用户评分，取值 1 到 5 星',
    comment            varchar(500)                          null comment '用户自由评价，最多 500 个字符',
    images             json                                  null comment '品茶照片地址数组；第一版可为空',
    created_at         datetime    default CURRENT_TIMESTAMP not null comment '记录创建时间',
    updated_at         datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '记录最后修改时间',
    constraint fk_record_user
        foreign key (user_id) references user (id)
            on delete cascade,
    constraint chk_record_price
        check ((`price` is null) or (`price` >= 0)),
    constraint chk_record_rating
        check (`rating` between 1 and 5)
)
    comment '用户品茶记录表';

create index idx_record_user_time
    on tea_record (user_id asc, created_at desc);

create index idx_tea_record_created_user
    on tea_record (created_at, user_id);
