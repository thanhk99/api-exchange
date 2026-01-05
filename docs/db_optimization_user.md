# PostgreSQL Optimization for User Module

Tài liệu này ghi lại các bước tối ưu hóa cho hệ thống User sử dụng PostgreSQL.

## 1. Công nghệ áp dụng
- **MVCC (Multi-Version Concurrency Control)**: Sử dụng để hỗ trợ login song song. Triển khai qua `@Version` trong Spring Data JPA.
- **B-tree Index**: Tối ưu hóa tìm kiếm email và username.
- **Partial Index**: Tạo index chỉ dành cho các user đang ở trạng thái `ACTIVE`.
- **RLS (Row Level Security)**: Bảo vệ dữ liệu ở mức hàng, đảm bảo user chỉ truy cập được dữ liệu của chính mình.

## 2. Chi tiết thực hiện
- **Model**: `api.exchange.models.User` đã được cập nhật với `@Version` và Index annotations.
- **SQL Script**: `postgresql_optimization.sql` chứa các lệnh cấu hình RLS và Partial Index.

## 3. Lợi ích
- Tăng tốc độ xác thực người dùng.
- Giảm lock tranh chấp khi cập nhật thông tin đăng nhập cuối cùng.
- Bảo mật dữ liệu tầng sâu nhất (Database level).
