-- Add merchant_id column to t_order table
ALTER TABLE t_order ADD COLUMN merchant_id BIGINT AFTER user_id;