create index idx_point_transaction_free_usage
    on user_point_transaction (user_id, type, status, business_key);
