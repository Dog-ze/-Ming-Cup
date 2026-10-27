-- 一盏茗：茶点规则改为“每个茶类一条固定搭配规则”。
-- 执行前请确认当前连接的是 yizhanming 数据库。
-- 本脚本可重复执行；旧表若按“茶类 + 气候”保存多条规则，会保留每个茶类 id 最大的一条后再写入下面的固定种子规则。

USE yizhanming;

CREATE TABLE IF NOT EXISTS pairing_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '本地茶食搭配规则主键 ID',
    tea_category VARCHAR(16) NOT NULL COMMENT '适用茶类，例如绿茶、红茶',
    foods JSON NOT NULL COMMENT '推荐食物名称数组',
    advice VARCHAR(255) NOT NULL COMMENT '日常搭配建议，不包含医疗承诺',
    UNIQUE KEY uk_pairing_rule_category (tea_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地茶食搭配规则表';

DELIMITER //

CREATE PROCEDURE migrate_pairing_rule_to_category_only()
BEGIN
    DECLARE climate_column_exists INT DEFAULT 0;
    DECLARE category_unique_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO climate_column_exists
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pairing_rule'
      AND column_name = 'climate_tag';

    IF climate_column_exists > 0 THEN
        -- 旧规则可能同一茶类有多行不同天气数据；删除旧行时保留 id 较大的一行。
        DELETE older_rule
        FROM pairing_rule older_rule
        INNER JOIN pairing_rule newer_rule
            ON older_rule.tea_category = newer_rule.tea_category
           AND older_rule.id < newer_rule.id;

        ALTER TABLE pairing_rule DROP COLUMN climate_tag;
    END IF;

    SELECT COUNT(*) INTO category_unique_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'pairing_rule'
      AND index_name = 'uk_pairing_rule_category';

    IF category_unique_exists = 0 THEN
        ALTER TABLE pairing_rule
            ADD UNIQUE KEY uk_pairing_rule_category (tea_category);
    END IF;
END //

CALL migrate_pairing_rule_to_category_only() //
DROP PROCEDURE migrate_pairing_rule_to_category_only //

DELIMITER ;

INSERT INTO pairing_rule(tea_category, foods, advice) VALUES
('绿茶', JSON_ARRAY('清淡糕点', '水果', '虾仁'), '绿茶滋味鲜爽，搭配清淡食物更容易感受茶香，避免过甜或油炸点心。'),
('白茶', JSON_ARRAY('水果', '低糖饼干', '蒸点心'), '白茶滋味清雅，适合搭配甜度较低、味道不过重的小食。'),
('黄茶', JSON_ARRAY('米糕', '坚果', '水果'), '黄茶口感温和，搭配清淡点心即可。'),
('青茶', JSON_ARRAY('凤梨酥', '坚果', '蒸点心'), '乌龙茶香气明显，搭配不抢味的点心更容易感受茶香。'),
('红茶', JSON_ARRAY('牛奶饼干', '全麦面包', '坚果'), '红茶口感醇和，可搭配温热、低糖的点心；饮茶浓度宜适中。'),
('黑茶', JSON_ARRAY('广式茶点', '坚果', '牛肉干'), '黑茶适合搭配油脂适中的茶点，晚间注意不要饮用过浓。'),
('花草', JSON_ARRAY('水果', '全麦饼干', '蒸点心'), '花草茶香气轻柔，搭配清爽、少糖的小食更合适。')
ON DUPLICATE KEY UPDATE
    foods = VALUES(foods),
    advice = VALUES(advice);

-- 执行完成后应恰好返回 7 行，且没有 climate_tag 字段。
SELECT id, tea_category, foods, advice
FROM pairing_rule
ORDER BY id;
