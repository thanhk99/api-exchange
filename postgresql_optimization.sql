-- 1. B-tree Index cho email và username (JPA đã tạo nhưng thêm ở đây để minh bạch)
-- CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
-- CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 2. Partial Index cho user ACTIVE
-- Giúp tăng tốc độ truy vấn cho các phiên làm việc của user đang hoạt động, 
-- và bỏ qua các user BANNED/PENDING trong index này để tiết kiệm dung lượng.
CREATE INDEX IF NOT EXISTS idx_users_active_status 
ON "user"(uid) 
WHERE user_status = 'ACTIVE';

-- 3. Row Level Security (RLS)
-- Bước A: Bật tính năng RLS trên bảng "user"
ALTER TABLE "user" ENABLE ROW LEVEL SECURITY;

-- Bước B: Tạo chính sách truy cập dữ liệu
-- Chính sách này đảm bảo một user chỉ có thể THẤY (SELECT) dữ liệu của chính họ
CREATE POLICY user_self_access_policy ON "user"
FOR SELECT
USING (uid = current_setting('app.current_user_id', true));

-- Bước C: Tạo chính sách thay đổi dữ liệu
-- Đảm bảo user chỉ có thể cập nhật dữ liệu của chính họ
CREATE POLICY user_self_update_policy ON "user"
FOR UPDATE
USING (uid = current_setting('app.current_user_id', true))
WITH CHECK (uid = current_setting('app.current_user_id', true));

-- Lưu ý: Trong ứng dụng Java, bạn cần chạy lệnh này sau khi lấy kết nối:
-- SET app.current_user_id = 'id_cua_user_dang_dang_nhap';
