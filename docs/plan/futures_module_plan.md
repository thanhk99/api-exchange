# Kế hoạch Phát triển Module Futures (Futures Trading Module)

## 1. Phạm vi (Scope)
Module Futures phức tạp hơn Spot do có các cơ chế đòn bẩy (Leverage), ký quỹ (Margin), và thanh lý (Liquidation).
Phạm vi bao gồm:
1.  Quản lý Vị thế (Positions).
2.  Tính toán Margin (Isolated/Cross).
3.  Funding Rate (Phí qua đêm/Funding fee cứ mỗi 8h).
4.  Cơ chế Thanh lý (Liquidation Engine).

## 2. Refactoring (Giai đoạn hiện tại)

### Mục tiêu
Cách ly hoàn toàn logic Futures khỏi Spot. Database của Futures (`futures_orders`, `futures_positions`) phải tách biệt.

### Cấu trúc Controller mới
*   **`PublicFuturesController.java`** (Đã có, cần clean up)
    *   API: `/api/v1/public/futures/market-data` (Mark price, Index price).
    *   API: `/api/v1/public/futures/funding-rate`.

*   **`FuturesTradingController.java`**
    *   API: `/api/v1/futures/position` (Điều chỉnh đòn bẩy, Margin).
    *   API: `/api/v1/futures/order` (Long/Short).

### Logic cần tách biệt
*   **Funding Rate Scheduler:** Cần một Job riêng chạy định kỳ (ví dụ sử dụng Quartz Scheduler) để tính và trừ/cộng tiền Funding Fee cho tất cả vị thế mở.
*   **Mark Price Calculation:** Logic tính giá tham chiếu (Mark Price) từ các sàn khác (Binance, OKX) để tránh thao túng giá (scam wicks).

## 3. Nâng cao Hiệu năng (Future Improvements)

### Liquidation Engine (Máy thanh lý)
*   Đây là component quan trọng nhất để bảo vệ sàn khỏi thua lỗ.
*   **Cơ chế:** Một process chạy background liên tục quét tất cả các Positions.
*   **Logic:** Nếu `Margin Ratio` <= `Maintenance Margin` -> Kích hoạt thanh lý.
*   **Tối ưu:** Sử dụng PriorityQueue để ưu tiên check các vị thế có rủi ro cao (Margin thấp) trước.

### Isolated vs Cross Margin
*   Hiện tại (giả định): Hệ thống code đơn giản.
*   Tương lai: Cần implement rõ ràng logic Isolated (tiền rủi ro gói gọn trong vị thế) và Cross (dùng toàn bộ ví futures để gồng lỗ).

### Position Sharding
*   Khi số lượng vị thế quá lớn (hàng triệu), database sẽ bị lock.
*   Giải pháp: Sharding bảng `positions` theo `userId` hoặc `symbol`.
