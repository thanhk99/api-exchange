# Wallet & Asset Module Optimization

Tài liệu này phác thảo các tiêu chuẩn an toàn tài chính áp dụng cho hệ thống tài khoản (Wallet).

## 1. Công nghệ PostgreSQL áp dụng
- **ACID**: Đảm bảo mọi giao dịch chuyển tiền đều toàn vẹn.
- **Serializable Isolation**: Chống lỗi Double Spend (chi tiêu trùng) bằng cách tuần tự hóa các giao dịch cạnh tranh.
- **Advisory Lock**: Sử dụng khóa tầng hệ thống cho các thao tác nhạy cảm như rút tiền (Withdraw).
- **Partial Index**: Tối ưu hóa truy vấn cho các ví đang hoạt động (`ACTIVE`).

## 2. Nguyên tắc Ledger (Sổ cái)
- **Append-only**: Không bao giờ cập nhật bản ghi cũ trong Ledger. Mọi biến động là một bản ghi mới.
- **Idempotency**: Sử dụng `idempotency_key` để tránh xử lý một yêu cầu hai lần.
- **Snapshot**: Bảng Wallet đóng vai trò là snapshot số dư hiện tại để truy vấn nhanh, nhưng Ledger mới là nguồn dữ liệu gốc.
- **Rebuild balance**: Hệ thống có khả năng tính toán lại số dư từ Ledger nếu cần thiết.

## 3. Checklist triển khai
- [x] Thêm Idempotency Key vào Ledger.
- [ ] Áp dụng Serializable Transaction.
- [ ] Triển khai Advisory Lock.
- [ ] Balance Snapshot sync với Ledger.
