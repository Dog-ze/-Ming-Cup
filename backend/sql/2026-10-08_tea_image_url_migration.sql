-- 为茶叶封面图片增加字段。可重复执行：若 image_url 已存在则不再添加。
USE yizhanming;

DELIMITER //

CREATE PROCEDURE add_tea_image_url_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'tea'
          AND COLUMN_NAME = 'image_url'
    ) THEN
        ALTER TABLE tea
            ADD COLUMN image_url VARCHAR(500) NULL COMMENT '茶叶封面图片访问地址' AFTER source;
    END IF;
END //

CALL add_tea_image_url_if_missing() //
DROP PROCEDURE add_tea_image_url_if_missing //

DELIMITER ;

SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'tea'
  AND COLUMN_NAME = 'image_url';


SHOW INDEX FROM tea_record;
SHOW INDEX FROM questionnaire;

CREATE INDEX idx_tea_record_created_user
    ON tea_record (created_at, user_id);

CREATE INDEX idx_questionnaire_updated_user
    ON questionnaire (updated_at, user_id);

SELECT INDEX_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'admin_operation_log'
  AND INDEX_NAME = 'idx_operation_log_module_action_time';

CREATE INDEX idx_operation_log_module_action_time
    ON admin_operation_log (module, action, created_at, id);