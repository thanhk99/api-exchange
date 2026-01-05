# Plan Refactoring: Chart Data & WebSocket System

## 1. Hiện trạng (Current State)

*   **Nguồn dữ liệu:** `SpotMarketWebSocket` đang đóng vai trò là Client kết nối đến Binance để lấy giá về, sau đó push thẳng vào `SimpMessagingTemplate`.
*   **Lưu trữ nến (Kline):**
    *   Realtime: Lưu trong RAM (`RingBufferService`, limit 72 nến).
    *   History: Tính toán từ Transaction History hoặc query DB (chậm).
*   **WebSocket Server:** Spring STOMP (`SimpleBroker`). Dễ triển khai nhưng khó scale > 10k connections.

## 2. Chiến lược Refactor Dữ liệu Biểu đồ (Chart Data Strategy)

Biểu đồ nến (Klines/Candlestick) là thành phần chịu tải cao nhất trên Frontend (TV Chart).

### A. Kiến trúc Lưu trữ (Storage)
1.  **Hot Data (Realtime - 24h qua):**
    *   Tiếp tục dùng **Redis** (Sorted Set) hoặc **In-Memory** cache.
    *   Cấu trúc Key: `kline:{symbol}:{interval}`.
    *   Score: Timestamp.
    *   Value: JSON/Protobuf của cây nến.
2.  **Cold Data (History - Vài tháng/năm):**
    *   Chuyển từ PostgreSQL/MySQL thường sang **Time Series Database (TSDB)**.
    *   Đề xuất: **TimescaleDB** (Extension của Postgres, dễ dùng) hoặc **InfluxDB**.
    *   Lý do: Query range time (`select * from kline where time > x and time < y`) nhanh gấp 100 lần DB thường.

### B. Logic Tổng hợp (Aggregation)
*   Không tính toán nến 1h, 4h từ nến 1m mỗi khi user request.
*   **Streaming Aggregation:**
    *   Khi có giá mới (Tick) -> Update nến 1s -> Update bộ nhớ đệm nến 1m.
    *   Khi nến 1m đóng cửa -> Async Job tính toán nến 1h/4h và lưu xuống DB.

## 3. Chiến lược Refactor WebSocket & API

### A. Tách "Consumer" và "Producer"
Hiện tại logic đang nối tắt: `Binance Stream` -> `Websocket Server`. Cần tách ra qua lớp trung gian.

1.  **Ingestion Service (Consumer):**
    *   Nhiệm vụ: Kết nối Binance/OKX, nhận data giá.
    *   Action: Normalize data -> Bắn vào **Kafka Topic** (`market-data-tick`).
2.  **WebSocket Push Service (Producer):**
    *   Nhiệm vụ: Subscribe Kafka Topic -> Push xuống Client.
    *   Lợi ích: Nếu WebSocket Server sập, Ingestion Service vẫn chạy bình thường. Có thể dựng 10 server WebSocket Push nấp sau Load Balancer.

### B. Tối ưu Giao thức (Protocol)
1.  **Compression:** Bật GZIP cho API response list nến (giảm size từ 500KB -> 50KB).
2.  **Throttling:** Chỉ push giá xuống Client mỗi 100ms/200ms (snapshot), không push từng tick (mắt người không nhìn kịp).
3.  **Binary Format (Tương lai):** Dùng Protobuf thay JSON để giảm payload size cho WebSocket.

## 4. Lộ trình Thực hiện (Implementation Steps)

### Phase 1: Ổn định (Stabilization)
- [ ] Refactor `SpotMarketWebSocket`: Tách logic xử lý data ra khỏi logic kết nối mạng.
- [ ] Implement Redis Cache cho API Kline History.

### Phase 2: Nâng cấp Storage
- [ ] Cài đặt TimescaleDB/InfluxDB.
- [ ] Viết Job migrate dữ liệu từ bảng cũ sang TSDB.
- [ ] Sửa `KlineCalculationService` để đọc từ TSDB.

### Phase 3: High Performance WebSocket
- [ ] Triển khai Kafka/Redis PubSub làm Message Broker.
- [ ] Tách WebSocket Server ra thành microservice riêng.
