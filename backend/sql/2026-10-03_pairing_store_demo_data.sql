-- 可选：仅用于本地开发演示。请先执行 2026-10-03_pairing_store_schema.sql。
USE yizhanming;

INSERT INTO pairing_store (
    store_name, address, longitude, latitude, phone,
    tea_categories, food_tags, description,
    promotion_score, promotion_enabled, status
) VALUES
('苏州茶香糕点铺', '江苏省苏州市姑苏区观前街附近', 120.6195850, 31.3113000, '13800000001',
 JSON_ARRAY('绿茶', '白茶'), JSON_ARRAY('清淡糕点', '低糖饼干', '米糕'),
 '清淡少糖的传统糕点，适合搭配绿茶或白茶。', 20, 1, 1),
('江南小食铺', '江苏省苏州市平江路附近', 120.6312000, 31.3195000, '13800000002',
 JSON_ARRAY('红茶', '黑茶', '青茶'), JSON_ARRAY('蒸点心', '坚果', '全麦面包'),
 '提供蒸点心和日常小食，适合下午品茶。', 0, 0, 1),
('果香轻食店', '江苏省苏州市工业园区附近', 120.7315000, 31.2998000, '13800000003',
 JSON_ARRAY('绿茶', '花草', '白茶'), JSON_ARRAY('水果', '坚果'),
 '以新鲜水果和坚果为主，适合清爽茶饮搭配。', 0, 0, 1);


CREATE TABLE admin_user (
                            id BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员主键 ID',
                            username VARCHAR(32) NOT NULL COMMENT '管理员登录账号',
                            password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的管理员密码',
                            display_name VARCHAR(32) NOT NULL COMMENT '后台显示名称',
                            status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：0 停用，1 启用',
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_admin_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员账号表';

CREATE TABLE admin_operation_log (
                                     id BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作日志主键 ID',
                                     admin_id BIGINT NOT NULL COMMENT '操作管理员 ID',
                                     module VARCHAR(32) NOT NULL COMMENT '模块：user、tea、pairing_rule、store 等',
                                     action VARCHAR(32) NOT NULL COMMENT '动作：create、update、enable、disable、promotion 等',
                                     target_id BIGINT NULL COMMENT '被操作对象 ID',
                                     detail JSON NULL COMMENT '操作前后必要字段摘要，不能记录密码和完整定位',
                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                     PRIMARY KEY (id),
                                     INDEX idx_admin_operation_time (admin_id, created_at),
                                     INDEX idx_operation_target (module, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员操作审计日志表';

ALTER TABLE `user`
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：0 停用，1 启用';

USE yizhanming;

INSERT INTO admin_user (
    username,
    password_hash,
    display_name,
    status
) VALUES (
             'admin',
             '$2a$10$Cb6ng.WvHxNHMEAoWbCQmOokqY3yRZUHZjC6UU4gye0CX2GqxQ/DS',
             '系统管理员',
             1
         );

SELECT INDEX_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user'
  AND INDEX_NAME = 'idx_user_created_at';

SELECT COLUMN_NAME FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'status';

SELECT INDEX_NAME FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_user_status_created_at';

ALTER TABLE tea
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '1上架，0下架',
    ADD COLUMN image_url VARCHAR(500) NULL COMMENT '图片地址',
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

CREATE INDEX idx_tea_status_category ON tea(status, category);

SELECT tea_category, COUNT(*) AS total
FROM pairing_rule
GROUP BY tea_category
HAVING COUNT(*) > 1;

ALTER TABLE pairing_rule
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    ADD UNIQUE KEY uk_pairing_rule_tea_category (tea_category);

ALTER TABLE tea
    ADD COLUMN image_url VARCHAR(500) NULL COMMENT '茶叶封面图片访问地址' AFTER source;