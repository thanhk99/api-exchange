# Ledger & Accounting Module Documentation

Tài liệu này hướng dẫn cách sử dụng và quản trị Module Sổ cái (Ledger) trong hệ thống API Exchange.

## 1. Tính bất biến (Immutability)
Mọi bản ghi trong các bảng history (`spot_wallet_history`, `funding_wallet_historys`) là bất biến. 
- **Quy tắc**: Không bao giờ sử dụng lệnh `UPDATE` hoặc `DELETE` trên các bảng này.
- **Bảo vệ**: Đã thiết lập Database Trigger `protect_ledger_immutability` để chặn mọi thao tác sửa đổi. Nếu có sai sót, phải tạo một bản ghi bù trừ (Reversal/Adjustment record).

## 2. Metadata (JSONB)
Trường `metadata` cho phép lưu trữ thông tin động mà không cần sửa đổi cấu trúc bảng:
- **Ví dụ**:
  ```json
  {
    "ip": "1.2.3.4",
    "device": "iPhone 15",
    "exchange_rate": 30500.5,
    "source": "mobile_app"
  }
  ```
- **Sử dụng**: Trong Java, bạn có thể truyền một `Map<String, Object>` vào trường này.

## 3. Partitioning
Hệ thống được thiết kế để hỗ trợ Table Partitioning theo tháng.
- **Lợi ích**: Tăng tốc độ truy vấn giao dịch gần đây và cho phép xóa dữ liệu cũ (archiving) cực nhanh bằng cách DROP partition.
- **Cấu hình**: Xem chi tiết trong file [ledger_setup.sql](file:///d:/project/api-exchange/ledger_setup.sql).

## 4. Kiểm toán (Audit)
- **Role**: `audit_user` được tạo để cấp cho bên thứ ba hoặc hệ thống kiểm toán độc lập.
- **Quyền hạn**: Chỉ đọc (Read-only) trên toàn bộ database.
- **Tính toàn vẹn**: Mỗi bản ghi có `reference_id` để liên kết chéo với các module khác (Orders, Transactions).

## 5. Metadata Indexing (Tùy chọn)
Nếu cần tìm kiếm theo metadata thường xuyên, bạn có thể tạo index JSONB:
```sql
CREATE INDEX idx_ledger_metadata_ip ON spot_wallet_history USING gin (metadata);
```
