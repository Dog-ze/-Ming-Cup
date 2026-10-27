-- 一盏茗：手机号、邮箱登录改造。
-- 执行前请确认当前连接的是 yizhanming 数据库，并先备份 user 表。
-- 原 username 字段保留为展示名称，可重复；phone 和 email 是新注册用户的唯一登录身份。

USE yizhanming;

DELIMITER //

CREATE PROCEDURE migrate_user_contact_login()
BEGIN
    DECLARE phone_column_exists INT DEFAULT 0;
    DECLARE email_column_exists INT DEFAULT 0;
    DECLARE username_unique_index VARCHAR(64) DEFAULT NULL;
    DECLARE phone_unique_exists INT DEFAULT 0;
    DECLARE email_unique_exists INT DEFAULT 0;

    -- 兼容不支持 ADD COLUMN IF NOT EXISTS 的 MySQL 版本。
    SELECT COUNT(*) INTO phone_column_exists
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'phone';
    IF phone_column_exists = 0 THEN
        ALTER TABLE `user` ADD COLUMN phone VARCHAR(20) NULL COMMENT '用户手机号，登录身份，唯一';
    END IF;

    SELECT COUNT(*) INTO email_column_exists
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'email';
    IF email_column_exists = 0 THEN
        ALTER TABLE `user` ADD COLUMN email VARCHAR(128) NULL COMMENT '用户邮箱，登录身份，唯一';
    END IF;

    -- 找出 username 上的单列唯一索引并删除，使用户名可以重复。
    SELECT s.index_name INTO username_unique_index
    FROM information_schema.statistics s
    WHERE s.table_schema = DATABASE()
      AND s.table_name = 'user'
      AND s.column_name = 'username'
      AND s.non_unique = 0
      AND s.index_name <> 'PRIMARY'
    LIMIT 1;

    IF username_unique_index IS NOT NULL THEN
        SET @drop_username_index = CONCAT('ALTER TABLE `user` DROP INDEX `', username_unique_index, '`');
        PREPARE statement FROM @drop_username_index;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;

    SELECT COUNT(*) INTO phone_unique_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'uk_user_phone';
    IF phone_unique_exists = 0 THEN
        ALTER TABLE `user` ADD UNIQUE KEY uk_user_phone (phone);
    END IF;

    SELECT COUNT(*) INTO email_unique_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'uk_user_email';
    IF email_unique_exists = 0 THEN
        ALTER TABLE `user` ADD UNIQUE KEY uk_user_email (email);
    END IF;
END //

CALL migrate_user_contact_login() //
DROP PROCEDURE migrate_user_contact_login //

DELIMITER ;

-- 老用户不会被删除，但 phone 和 email 会是 NULL，暂时不能使用新登录方式。
-- 请在上线前为老用户补齐手机号和邮箱，或让他们重新注册测试账号。
SELECT id, username, phone, email FROM `user` ORDER BY id;


ALTER TABLE `user`
    MODIFY COLUMN nickname VARCHAR(32) NULL DEFAULT NULL;
