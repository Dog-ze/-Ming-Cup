-- 一盏茗：为用户资料增加头像地址。
-- 执行前请确认当前连接的是 yizhanming 数据库。
-- 此写法兼容不支持 “ADD COLUMN IF NOT EXISTS” 的 MySQL 版本；重复执行也不会报错。

USE yizhanming;

DELIMITER //

CREATE PROCEDURE add_user_avatar_column()
BEGIN
    DECLARE avatar_column_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO avatar_column_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'avatar';

    IF avatar_column_exists = 0 THEN
        ALTER TABLE `user` ADD COLUMN avatar VARCHAR(255) NULL COMMENT '用户头像访问地址';
    END IF;
END //

CALL add_user_avatar_column() //
DROP PROCEDURE add_user_avatar_column //

DELIMITER ;

