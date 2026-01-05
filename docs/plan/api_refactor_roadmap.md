# Lộ trình Refactor API (API Refactor Roadmap)

Đây là tài liệu tổng hợp lộ trình thực hiện việc refactor code theo các bản kế hoạch chi tiết trong thư mục `docs/plan`.

## Bước 1: Chuẩn bị & Cấu trúc (Tuần 1)
- [ ] Tạo các package mới trong source code để phân chia module rõ ràng:
    - `api.exchange.modules.spot`
    - `api.exchange.modules.futures`
    - `api.exchange.modules.market`
    - `api.exchange.modules.wallet`
- [ ] Định nghĩa lại `SecurityConfig` để hỗ trợ pattern URL mới `/api/v1/public/**`.

## Bước 2: Refactor Market Data (Tuần 1-2)
*Xem chi tiết: [market_data_wallet_plan.md](./market_data_wallet_plan.md)*
- [ ] Tạo `PublicCoinController`.
- [ ] Di chuyển logic từ `CoinDataController` và `PriceCoinHistoryController` sang.
- [ ] Kiểm tra và xóa các controller cũ.

## Bước 3: Refactor Spot Module (Tuần 2-3)
*Xem chi tiết: [spot_module_plan.md](./spot_module_plan.md)*
- [ ] Tạo `PublicSpotController` (chỉ read data).
- [ ] Di chuyển logic Kline và Orderbook của Spot sang controller mới.
- [ ] Đổi URL frontend để trỏ về API mới.

## Bước 4: Refactor Futures Module (Tuần 3-4)
*Xem chi tiết: [futures_module_plan.md](./futures_module_plan.md)*
- [ ] Nâng cấp `PublicFuturesController`.
- [ ] Gộp `FuturesKlineController` vào.
- [ ] Đảm bảo tính tách biệt của logic Futures.

## Bước 5: Kiểm thử & Triển khai
- [ ] Chạy full regression test (test hồi quy) để đảm bảo không gãy chức năng cũ.
- [ ] Cập nhật tài liệu API (Swagger/Postman) cho team Frontend.

---
**Tài liệu tham khảo:**
1. [Kiến trúc Tổng thể](./general_architecture.md)
2. [Spot Module Plan](./spot_module_plan.md)
3. [Futures Module Plan](./futures_module_plan.md)
4. [Message & Wallet Plan](./market_data_wallet_plan.md)
