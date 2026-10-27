# 一盏茗后端数据库迁移发布清单

## 首次接入 Flyway

1. 备份目标数据库，并确认备份文件可读取。
2. 在数据库备份副本中设置 `FLYWAY_ENABLED=true`，不要先操作正式库。
3. 启动后端后执行：

   ```sql
   SELECT version, description, type, success
   FROM flyway_schema_history
   ORDER BY installed_rank;
   ```

4. 已有数据库预期只有一条成功记录：`1 / << Flyway Baseline >> / BASELINE / 1`。
5. 确认原有业务表数量没有变化，管理员登录、用户登录和图片访问正常。

## 验证空数据库

1. 新建空测试库，例如 `yizhanming_flyway_empty_test`。
2. 将 `DB_URL` 指向该空库，并设置 `FLYWAY_ENABLED=true`。
3. 启动后端；Flyway 应执行 `V1__baseline_current_schema.sql` 并创建全部表。
4. 确认 `flyway_schema_history` 中存在 `V1` 的成功 SQL 迁移记录。

## 后续数据库修改

1. 在 `src/main/resources/db/migration/` 新建下一个连续版本，例如 `V2__add_store_business_hours.sql`。
2. SQL 只能描述本次新增或修改，不能修改已经发布的 V1 文件。
3. 不在迁移文件中写真实用户数据、密码、地图 Key 或本机路径。
4. 先在空测试库和已有数据的备份副本中各验证一次，再发布。

## 正式发布

1. 再次备份正式数据库和上传图片目录。
2. 检查 `DB_URL` 指向正式数据库，且只在本次发布环境设置 `FLYWAY_ENABLED=true`。
3. 部署并查看启动日志；迁移失败时停止发布，不手工修改 `flyway_schema_history`。
4. 验证数据库历史表、管理员登录、普通用户登录、图片访问和附近店铺接口。
5. 本次版本须确认以下迁移均为成功状态：
   - `V2__add_admin_token_revocation_and_recommend_index.sql`：后台退出登录所需的撤销表。
   - `V3__add_user_point_account_and_transaction.sql`：积分账户和积分流水表。
   - `V4__add_point_free_usage_index.sql`：AI 每日免费次数的查询索引。
6. 启动后用普通账号依次验证：领取每日积分、首次 AI 泡法、第二次 AI 泡法扣分，以及积分余额查询。

## 回滚原则

1. 不执行 `flyway clean`；项目配置已经禁用该危险操作。
2. 如新版本无法启动，先回滚后端 Jar 和前端静态文件。
3. 如迁移本身不可逆，从发布前数据库备份恢复，并记录原因。
