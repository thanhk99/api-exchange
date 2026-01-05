-- ==========================================================
-- LEDGER & ACCOUNTING DATABASE SETUP (POSTGRESQL)
-- ==========================================================

-- 1. TẠO TRIGGER NGĂN CHẶN CẬP NHẬT/XÓA (TRÍNH TÍNH BẤT BIẾN)
-- Mọi dữ liệu trong Ledger (History) chỉ được phép thêm (INSERT).

CREATE OR REPLACE FUNCTION protect_ledger_immutability()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Ledger records are immutable. UPDATE and DELETE operations are prohibited.';
END;
$$ LANGUAGE plpgsql;

-- Áp dụng cho bảng Spot Wallet History
CREATE TRIGGER trg_protect_spot_ledger
BEFORE UPDATE OR DELETE ON spot_wallet_history
FOR EACH ROW EXECUTE FUNCTION protect_ledger_immutability();

-- Áp dụng cho bảng Funding Wallet History
CREATE TRIGGER trg_protect_funding_ledger
BEFORE UPDATE OR DELETE ON funding_wallet_historys
FOR EACH ROW EXECUTE FUNCTION protect_ledger_immutability();


-- 2. THIẾT LẬP TABLE PARTITIONING (THEO THỜI GIAN)
-- Lưu ý: Việc chuyển đổi bảng hiện có sang partitioned table yêu cầu quy trình migration phức tạp hơn.
-- Đây là cấu trúc mẫu để tạo bảng mới được chia vùng.

/*
DROP TABLE IF EXISTS wallet_history_partitioned;
CREATE TABLE wallet_history_partitioned (
    id BIGSERIAL,
    user_id VARCHAR(255) NOT NULL,
    asset VARCHAR(10) NOT NULL,
    amount DECIMAL(24,8) NOT NULL,
    create_dt TIMESTAMP NOT NULL,
    metadata JSONB
) PARTITION BY RANGE (create_dt);

-- Tạo Partition cho tháng 1/2026
CREATE TABLE wallet_history_2026_01 PARTITION OF wallet_history_partitioned
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
*/


-- 3. TẠO ROLE AUDIT (READ-ONLY)
-- Role này chỉ dành cho việc đối soát, không có quyền thay đổi bất kỳ dữ liệu nào.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'audit_user') THEN
        CREATE ROLE audit_user WITH LOGIN PASSWORD 'strong_audit_password';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE current_database() TO audit_user;
GRANT USAGE ON SCHEMA public TO audit_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO audit_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO audit_user;


-- 4. BẬT LOGICAL REPLICATION IDENTITY
-- Chuẩn bị cho việc đồng bộ dữ liệu sang hệ thống audit tách biệt (nếu cần).
ALTER TABLE spot_wallet_history REPLICA IDENTITY FULL;
ALTER TABLE funding_wallet_historys REPLICA IDENTITY FULL;
