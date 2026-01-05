# Kế hoạch Market Data & Wallet (Common Modules)

## 1. Module Market Data (Coin & Prices)

Đây là module cung cấp thông tin "tĩnh" và các dữ liệu tham chiếu chung cho cả Spot và Futures.

### Refactoring
*   **`PublicCoinController`**:
    *   Cung cấp danh sách Coin được hỗ trợ, network nạp rút.
    *   Cung cấp tỷ giá hối đoái (Exchange Rate) cho các cặp tiền tệ Fiat.

### Future Improvements
*   **Price Oracle:** Xây dựng một service chuyên đi "cào" (crawl) giá từ Binance, CoinGecko về để làm giá tham khảo.
*   **Global Cache:** Sử dụng Redis để cache config của Coin (min withdraw, min deposit, fee) để không phải query DB liên tục.

## 2. Module Wallet (Ví & Tài sản)

Module quan trọng nhất liên quan đến tiền của user.

### Hiện trạng
Code quản lý ví đang phân tán trong `SpotService`, `SpotWalletService`, `FundingWalletService`. Logic cộng trừ tiền chưa được chuẩn hóa.

### Kế hoạch Refactor (Ngắn hạn)
*   Quy hoạch lại về một `WalletService` duy nhất (hoặc Facade pattern) để gọi.
*   Đảm bảo mọi giao dịch thay đổi số dư biến động đều phải ghi log (`WalletHistory`) trước hoặc trong cùng transaction.

### Future Architecture (Dài hạn)
*   **Service Tách biệt:** Tách `WalletService` ra thành Microservice riêng.
*   **Giao tiếp Async:** Matching Engine sau khi khớp lệnh sẽ bắn event `TradeExecuted` vào Kafka. `WalletService` consume event này để trừ tiền Seller, cộng tiền Buyer.
    *   *Lợi ích:* Matching Engine không bị chậm vì chờ DB update ví.
    *   *Rủi ro:* Cần cơ chế xử lý sai lệch (Reconciliation).

### Hot & Cold Wallet System
*   Xây dựng hệ thống tự động quét ví nóng (Hot wallet) của user.
*   Khi số dư ví nóng quá lớn -> Tự động chuyển về ví lạnh (Cold wallet) của sàn.
*   Khi user rút tiền -> Nếu ví nóng thiếu, admin chuyển từ ví lạnh sang.
