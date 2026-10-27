-- 一盏茗：附近茶食店铺表与推广排序字段。
-- 可重复执行；适用于尚未建立 pairing_store 表、或已有旧版 pairing_store 表两种情况。
-- 执行前请确认当前连接的是 yizhanming 数据库。

USE yizhanming;

CREATE TABLE IF NOT EXISTS pairing_store (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '店铺编号',
    store_name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '店铺封面图片访问地址',
    address VARCHAR(255) NOT NULL COMMENT '店铺详细地址',
    longitude DECIMAL(10, 7) DEFAULT NULL COMMENT '店铺经度',
    latitude DECIMAL(10, 7) DEFAULT NULL COMMENT '店铺纬度',
    phone VARCHAR(20) DEFAULT NULL COMMENT '店铺联系电话',
    tea_categories JSON NOT NULL COMMENT '适配茶类 JSON 数组',
    food_tags JSON NOT NULL COMMENT '推荐食物标签 JSON 数组',
    description VARCHAR(500) DEFAULT NULL COMMENT '店铺推荐说明',
    promotion_score INT NOT NULL DEFAULT 0 COMMENT '推广排序分数，仅后台使用，建议 0 到 30',
    promotion_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用推广：0 否，1 是',
    promotion_start_at DATETIME DEFAULT NULL COMMENT '推广开始时间，为空表示立即生效',
    promotion_end_at DATETIME DEFAULT NULL COMMENT '推广结束时间，为空表示不设结束时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '展示状态：0 下架，1 上架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    PRIMARY KEY (id),
    INDEX idx_pairing_store_status (status),
    INDEX idx_pairing_store_location (longitude, latitude),
    INDEX idx_pairing_store_promotion (promotion_enabled, promotion_start_at, promotion_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='一盏茗茶食搭配店铺表';

-- 若表来自旧版脚本，则补齐推广字段。使用存储过程兼容旧版 MySQL。
DELIMITER //

CREATE PROCEDURE upgrade_pairing_store_promotion()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    DECLARE index_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO column_exists FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pairing_store' AND COLUMN_NAME = 'promotion_score';
    IF column_exists = 0 THEN
        ALTER TABLE pairing_store ADD COLUMN promotion_score INT NOT NULL DEFAULT 0 COMMENT '推广排序分数，仅后台使用，建议 0 到 30';
    END IF;

    SELECT COUNT(*) INTO column_exists FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pairing_store' AND COLUMN_NAME = 'promotion_enabled';
    IF column_exists = 0 THEN
        ALTER TABLE pairing_store ADD COLUMN promotion_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用推广：0 否，1 是';
    END IF;

    SELECT COUNT(*) INTO column_exists FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pairing_store' AND COLUMN_NAME = 'promotion_start_at';
    IF column_exists = 0 THEN
        ALTER TABLE pairing_store ADD COLUMN promotion_start_at DATETIME DEFAULT NULL COMMENT '推广开始时间，为空表示立即生效';
    END IF;

    SELECT COUNT(*) INTO column_exists FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pairing_store' AND COLUMN_NAME = 'promotion_end_at';
    IF column_exists = 0 THEN
        ALTER TABLE pairing_store ADD COLUMN promotion_end_at DATETIME DEFAULT NULL COMMENT '推广结束时间，为空表示不设结束时间';
    END IF;

    SELECT COUNT(*) INTO index_exists FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pairing_store' AND INDEX_NAME = 'idx_pairing_store_promotion';
    IF index_exists = 0 THEN
        ALTER TABLE pairing_store ADD INDEX idx_pairing_store_promotion (promotion_enabled, promotion_start_at, promotion_end_at);
    END IF;
END //

CALL upgrade_pairing_store_promotion() //
DROP PROCEDURE upgrade_pairing_store_promotion //

DELIMITER ;
