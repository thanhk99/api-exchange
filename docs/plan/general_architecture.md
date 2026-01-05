# Kế hoạch Kiến trúc Tổng thể (General Architecture Plan)

Tài liệu này định hướng lộ trình phát triển nền tảng từ mô hình Monolith hiện tại sang kiến trúc Microservices hiệu năng cao (High Frequency Trading).

## 1. Tầm nhìn Kiến trúc (Architectural Vision)

Mục tiêu là xây dựng một hệ thống có khả năng xử lý hàng triệu giao dịch mỗi giây (TPS) với độ trễ thấp (low latency), tương tự như các sàn lớn (OKX, Binance).

### Sơ đồ chuyển đổi

#### Giai đoạn 1: Modular Monolith (Hiện tại - Refactoring)
*   **Mô tả:** Codebase vẫn nằm trong một project Spring Boot duy nhất nhưng được tổ chức chặt chẽ theo module (Spot, Futures, Market).
*   **Mục tiêu:** Làm sạch code, tách sự phụ thuộc, chuẩn bị cho việc xé nhỏ service.
*   **Database:** Dùng chung 1 DB PostgreSQL/MySQL.

#### Giai đoạn 2: Tách Dịch vụ Dữ liệu (Market Data Services)
*   **Mô tả:** Tách riêng các API đọc dữ liệu (Chart, Price, Orderbook) ra khỏi API đặt lệnh (Trading).
*   **Công nghệ:**
    *   **Public API Gateway:** Nginx/Spring Cloud Gateway.
    *   **Caching:** Redis Cluster cache full orderbook/klines.
    *   **DB:** Bắt đầu chuyển dữ liệu nến (Kline) sang InfluxDB.

#### Giai đoạn 3: Microservices & Matching Engine
*   **Mô tả:** Hệ thống phân tán hoàn toàn.
*   **Thành phần Matching Engine:** Viết lại bằng Java (LMAX Disruptor) hoặc Go/Rust, chạy In-Memory hoàn toàn.
*   **Giao tiếp:** Sử dụng Kafka/RabbitMQ cho giao tiếp bất đồng bộ (Async).
*   **Wallet:** Service riêng biệt quản lý tiền, đảm bảo tính ACID cao nhất.

## 2. Các thành phần lõi (Core Components)

### 2.1. API Gateway Layer
*   **Public Gateway:** Rate limit, DDoS protection, Cache response.
*   **Private Gateway (Trading):** Auth (JWT), Signature verification, Route request to Engine.

### 2.2. Trading Engine Layer (Layer quan trọng nhất)
*   **Spot Engine:** Xử lý khớp lệnh Spot.
*   **Futures Engine:** Xử lý khớp lệnh Phái sinh, tính toán Funding Rate, Liquidation (Thanh lý).
*   **Risk Engine:** Kiểm tra ký quỹ (Margin check) trước khi đẩy lệnh vào Matching Engine.

### 2.3. Data & Storage Layer
*   **Hot Storage (Redis):** Session user, Orderbook snapshot, Recent trades.
*   **Warm Storage (RDBMS):** User info, Wallet balance, Order history.
*   **Cold Storage (Data Lake):** Logs, Archived orders (cho Reports/Audit).

## 3. Tech Stack Đề xuất
*   **Backend:** Java (Spring Boot) cho logic nghiệp vụ, Go/Rust cho modules cần high-performance.
*   **Message Queue:** Apache Kafka.
*   **Cache:** Redis Cluster.
*   **Database:** PostgreSQL (Transactional), ClickHouse/TimescaleDB (Analytics/Charts).
*   **Infrastructure:** Kubernetes (K8s), Docker.
