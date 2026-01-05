# Kế hoạch Phát triển Module Spot (Spot Trading Module)

## 1. Phạm vi (Scope)
Module này chịu trách nhiệm quản lý toàn bộ vòng đời giao dịch Giao ngay (Spot Trading):
1.  Đặt lệnh (Limit, Market, Stop-Limit).
2.  Hủy lệnh.
3.  Khớp lệnh (Matching).
4.  Cung cấp dữ liệu Orderbook và Kline cho Spot.

## 2. Refactoring (Giai đoạn hiện tại)

### Mục tiêu
Tách code Spot ra khỏi các service hỗn hợp hiện tại (`SpotService` hiện đang dính dáng logic ví và websocket lẫn lộn).

### Cấu trúc Controller mới
*   **`d:\project\api-exchange\src\main\java\api\exchange\controllers\PublicSpotController.java`**
    *   API: `GET /api/v1/public/spot/orderbook/{symbol}`
    *   API: `GET /api/v1/public/spot/kline`
    *   Nhiệm vụ: Chỉ đọc dữ liệu từ Cache/DB, không xử lý logic đặt lệnh.

*   **`d:\project\api-exchange\src\main\java\api\exchange\controllers\SpotTradingController.java`** (Private)
    *   API: `POST /api/v1/spot/order` (Đặt lệnh)
    *   API: `DELETE /api/v1/spot/order` (Hủy lệnh)
    *   Nhiệm vụ: Gọi `SpotMatchingService`.

### Cấu trúc Service mới (Đề xuất)
*   **`SpotMatchingService`**: Chỉ chứa logic tìm Order counter-party và khớp.
*   **`SpotOrderService`**: CRUD Order vào database, validate đầu vào.
*   **`SpotEventPublisher`**: Bắn event sau khi khớp lệnh (để update ví, bắn websocket).

## 3. Nâng cao Hiệu năng (Future Improvements)

### In-Memory Orderbook
*   Hiện tại: Query DB để tìm lệnh khớp -> Rất chậm.
*   Cải tiến: Load toàn bộ Active Order lên `TreeMap` (Java) hoặc `Redis ZSet`.
    *   Key: Price.
    *   Value: List of Orders.
*   Khi có lệnh mới, check ngay trên RAM để khớp.

### Batch Processing
*   Không save từng lệnh vào DB ngay lập tức.
*   Gom các thay đổi (Order Match, Balance Change) thành batch và ghi xuống DB mỗi 100ms hoặc khi đủ 1000 records.

### WebSocket Optimizations
*   Spot Data stream (Giá, Orderbook diff) cần được tách ra microservice riêng gọi là `Spot-Pusher`.
*   Service này subscribe Kafka topic `spot-events` và đẩy xuống user qua WebSocket.
