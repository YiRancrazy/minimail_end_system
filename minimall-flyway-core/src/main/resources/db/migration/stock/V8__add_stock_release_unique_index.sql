ALTER TABLE t_stock_journal
    ADD UNIQUE KEY uk_stock_journal_release_idem (sku_id, order_no, type, is_deleted);
