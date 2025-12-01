-- ==================================================================================
-- SCRIPT TẠO PARTITIONING CHO BẢNG futures_kline_data_1s (PostgreSQL)
-- ==================================================================================
-- Hướng dẫn:
-- 1. Chạy script này trên database PostgreSQL của bạn.
-- 2. Script sẽ đổi tên bảng cũ thành _backup và tạo bảng mới có tính năng Partitioning.
-- 3. Dữ liệu sẽ được chia theo tháng (Range Partitioning theo start_time).
-- ==================================================================================

-- 1. Đổi tên bảng hiện tại (nếu có) để backup
ALTER TABLE IF EXISTS futures_kline_data_1s RENAME TO futures_kline_data_1s_backup;

-- 2. Tạo bảng chính (Partitioned Table)
-- Lưu ý: Primary Key phải bao gồm column dùng để partition (start_time)
CREATE TABLE futures_kline_data_1s (
    id BIGSERIAL,
    symbol VARCHAR(20) NOT NULL,
    open_price NUMERIC(20, 8),
    close_price NUMERIC(20, 8),
    high_price NUMERIC(20, 8),
    low_price NUMERIC(20, 8),
    volume NUMERIC(20, 8),
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    close_time TIMESTAMP WITHOUT TIME ZONE,
    is_closed BOOLEAN,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    PRIMARY KEY (id, start_time)
) PARTITION BY RANGE (start_time);

-- 3. Tạo các Partition (Ví dụ cho các tháng tới)
-- Bạn cần tạo thêm partition cho các tháng tương lai định kỳ (hoặc dùng pg_partman)

-- Partition cho tháng 12/2025
CREATE TABLE futures_kline_data_1s_2025_12 PARTITION OF futures_kline_data_1s
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- Partition cho tháng 01/2026
CREATE TABLE futures_kline_data_1s_2026_01 PARTITION OF futures_kline_data_1s
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- Partition cho tháng 02/2026
CREATE TABLE futures_kline_data_1s_2026_02 PARTITION OF futures_kline_data_1s
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

-- 4. Tạo Index để tối ưu query
-- Index theo Symbol và Time (quan trọng nhất cho việc vẽ chart)
CREATE INDEX idx_futures_kline_1s_symbol_time ON futures_kline_data_1s (symbol, start_time DESC);

-- 5. Migrate dữ liệu cũ (Nếu cần)
-- INSERT INTO futures_kline_data_1s (symbol, open_price, close_price, high_price, low_price, volume, start_time, close_time, is_closed, created_at)
-- SELECT symbol, open_price, close_price, high_price, low_price, volume, start_time, close_time, is_closed, created_at
-- FROM futures_kline_data_1s_backup;
