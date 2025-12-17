# Sổ Lệnh Futures (Futures Order Book)

## Mục Lục
1. [Giới Thiệu](#giới-thiệu)
2. [Cấu Trúc Order Book](#cấu-trúc-order-book)
3. [Cơ Chế Khớp Lệnh](#cơ-chế-khớp-lệnh)
4. [API Order Book](#api-order-book)
5. [WebSocket Real-time Updates](#websocket-real-time-updates)
6. [Ví Dụ Thực Tế](#ví-dụ-thực-tế)

---

## Giới Thiệu

**Order Book** (Sổ lệnh) là danh sách tất cả các lệnh mua (bids) và lệnh bán (asks) đang chờ khớp cho một cặp giao dịch cụ thể. Đây là thành phần cốt lõi của hệ thống giao dịch, giúp người dùng:

- 📊 Xem độ sâu thị trường (market depth)
- 💰 Tìm giá tốt nhất để giao dịch
- 📈 Phân tích thanh khoản
- 🎯 Đặt lệnh limit hiệu quả

### Trạng Thái Hiện Tại

> ⚠️ **QUAN TRỌNG**: Hệ thống hiện tại **CHƯA CÓ** Order Book thực sự. 

**Cơ chế hiện tại:**
- Lệnh MARKET được khớp ngay lập tức với giá thị trường từ `CoinDataService`
- Lệnh LIMIT được scheduler kiểm tra mỗi giây và khớp khi giá thị trường đạt điều kiện
- Không có matching engine thực sự giữa các lệnh của người dùng

**Cơ chế lý tưởng (cần triển khai):**
- Order Book lưu trữ tất cả lệnh LIMIT đang chờ
- Matching Engine khớp lệnh giữa người mua và người bán
- Real-time updates qua WebSocket

---

## Cấu Trúc Order Book

### Thành Phần Chính

```
Order Book
├── Bids (Lệnh Mua)
│   ├── Price Level 1: [Price, Quantity, Orders]
│   ├── Price Level 2: [Price, Quantity, Orders]
│   └── ...
└── Asks (Lệnh Bán)
    ├── Price Level 1: [Price, Quantity, Orders]
    ├── Price Level 2: [Price, Quantity, Orders]
    └── ...
```

### Ví Dụ Trực Quan

```
BTCUSDT Order Book

ASKS (Lệnh Bán - Giá thấp nhất ở dưới)
┌─────────────┬──────────┬──────────┐
│    Price    │ Quantity │  Total   │
├─────────────┼──────────┼──────────┤
│  45,020.00  │   1.500  │  1.500   │
│  45,010.00  │   0.700  │  2.200   │
│  45,000.00  │   0.300  │  2.500   │ ← Best Ask (Giá bán tốt nhất)
└─────────────┴──────────┴──────────┘
        ↕ Spread: 50 USDT (0.11%)
┌─────────────┬──────────┬──────────┐
│  44,950.00  │   0.500  │  0.500   │ ← Best Bid (Giá mua tốt nhất)
│  44,940.00  │   1.200  │  1.700   │
│  44,930.00  │   0.800  │  2.500   │
└─────────────┴──────────┴──────────┘
BIDS (Lệnh Mua - Giá cao nhất ở trên)
```

### Thuật Ngữ Quan Trọng

| Thuật ngữ | Giải thích |
|-----------|------------|
| **Best Bid** | Giá mua cao nhất hiện tại |
| **Best Ask** | Giá bán thấp nhất hiện tại |
| **Spread** | Chênh lệch giữa Best Ask và Best Bid |
| **Market Depth** | Tổng khối lượng lệnh ở các mức giá |
| **Price Level** | Một mức giá cụ thể trong order book |
| **Liquidity** | Khả năng mua/bán nhanh mà không ảnh hưởng giá |

---

## Cơ Chế Khớp Lệnh

### 1. Cơ Chế Hiện Tại (Scheduler-based)

**File**: `FuturesOrderService.java`

```java
@Scheduled(fixedRate = 1000) // Chạy mỗi 1 giây
@Transactional
public void matchLimitOrders() {
    // Lấy tất cả lệnh đang chờ
    List<FuturesOrder> pendingOrders = futuresOrderRepository.findAll();
    
    for (FuturesOrder order : pendingOrders) {
        // Bỏ qua lệnh không phải PENDING hoặc MARKET
        if (order.getStatus() != OrderStatus.PENDING) continue;
        if (order.getType() == OrderType.MARKET) continue;
        
        // Lấy giá thị trường hiện tại
        BigDecimal currentPrice = coinDataService.getCurrentPrice(order.getSymbol());
        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) continue;
        
        boolean shouldExecute = false;
        
        // Logic khớp lệnh
        if (order.getSide() == OrderSide.BUY) {
            // Buy Limit: Khớp khi giá thị trường <= giá đặt
            if (currentPrice.compareTo(order.getPrice()) <= 0) {
                shouldExecute = true;
            }
        } else {
            // Sell Limit: Khớp khi giá thị trường >= giá đặt
            if (currentPrice.compareTo(order.getPrice()) >= 0) {
                shouldExecute = true;
            }
        }
        
        if (shouldExecute) {
            System.out.println("⚡ MATCHED Limit Order: " + order.getId() 
                + " Symbol: " + order.getSymbol() + " Price: " + currentPrice);
            futuresTradingService.executeOrder(order, currentPrice);
        }
    }
}
```

**Ưu điểm:**
- ✅ Đơn giản, dễ triển khai
- ✅ Phù hợp với MVP

**Nhược điểm:**
- ❌ Không có matching giữa các lệnh của người dùng
- ❌ Phụ thuộc vào giá thị trường bên ngoài
- ❌ Không tối ưu về hiệu suất
- ❌ Không có order book thực sự

### 2. Cơ Chế Lý Tưởng (Matching Engine)

**Nguyên tắc khớp lệnh:**

#### Price-Time Priority (Ưu tiên Giá-Thời gian)

1. **Ưu tiên giá**: Lệnh có giá tốt hơn được khớp trước
   - Lệnh MUA: Giá cao hơn ưu tiên
   - Lệnh BÁN: Giá thấp hơn ưu tiên

2. **Ưu tiên thời gian**: Nếu cùng giá, lệnh đặt trước được khớp trước

#### Ví Dụ Khớp Lệnh

**Tình huống ban đầu:**
```
Order Book:
Asks: 45,000 (0.5 BTC), 45,010 (0.3 BTC)
Bids: 44,950 (0.4 BTC), 44,940 (0.6 BTC)
```

**Lệnh mới: Market Buy 0.7 BTC**

Bước 1: Khớp với Ask tốt nhất (45,000)
- Khớp: 0.5 BTC @ 45,000
- Còn lại: 0.2 BTC

Bước 2: Khớp với Ask tiếp theo (45,010)
- Khớp: 0.2 BTC @ 45,010
- Hoàn thành

**Kết quả:**
```
Order Book sau khi khớp:
Asks: 45,010 (0.1 BTC)
Bids: 44,950 (0.4 BTC), 44,940 (0.6 BTC)

Lệnh đã khớp:
- 0.5 BTC @ 45,000
- 0.2 BTC @ 45,010
Giá trung bình: 45,002.86
```

### 3. Implementation Matching Engine (Đề Xuất)

```java
@Service
public class FuturesMatchingEngine {
    
    @Autowired
    private FuturesOrderRepository orderRepository;
    
    @Autowired
    private FuturesTradingService tradingService;
    
    /**
     * Khớp lệnh mới với order book
     */
    @Transactional
    public void matchOrder(FuturesOrder newOrder) {
        if (newOrder.getType() == OrderType.MARKET) {
            matchMarketOrder(newOrder);
        } else {
            matchLimitOrder(newOrder);
        }
    }
    
    /**
     * Khớp lệnh Market
     */
    private void matchMarketOrder(FuturesOrder marketOrder) {
        BigDecimal remainingQty = marketOrder.getQuantity();
        
        // Lấy lệnh đối diện (nếu BUY thì lấy SELL, và ngược lại)
        OrderSide oppositeSide = marketOrder.getSide() == OrderSide.BUY 
            ? OrderSide.SELL : OrderSide.BUY;
        
        // Sắp xếp: SELL theo giá tăng dần, BUY theo giá giảm dần
        Sort sort = oppositeSide == OrderSide.SELL 
            ? Sort.by("price").ascending() 
            : Sort.by("price").descending();
        
        List<FuturesOrder> oppositeOrders = orderRepository
            .findBySymbolAndSideAndStatusAndType(
                marketOrder.getSymbol(),
                oppositeSide,
                OrderStatus.PENDING,
                OrderType.LIMIT,
                sort
            );
        
        for (FuturesOrder limitOrder : oppositeOrders) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal matchQty = remainingQty.min(limitOrder.getQuantity());
            BigDecimal matchPrice = limitOrder.getPrice();
            
            // Thực hiện khớp
            executeTrade(marketOrder, limitOrder, matchQty, matchPrice);
            
            remainingQty = remainingQty.subtract(matchQty);
        }
        
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Insufficient liquidity");
        }
    }
    
    /**
     * Khớp lệnh Limit
     */
    private void matchLimitOrder(FuturesOrder limitOrder) {
        // Kiểm tra xem có lệnh đối diện phù hợp không
        OrderSide oppositeSide = limitOrder.getSide() == OrderSide.BUY 
            ? OrderSide.SELL : OrderSide.BUY;
        
        List<FuturesOrder> matchableOrders = findMatchableOrders(
            limitOrder.getSymbol(),
            oppositeSide,
            limitOrder.getPrice(),
            limitOrder.getSide()
        );
        
        BigDecimal remainingQty = limitOrder.getQuantity();
        
        for (FuturesOrder oppositeOrder : matchableOrders) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal matchQty = remainingQty.min(oppositeOrder.getQuantity());
            BigDecimal matchPrice = oppositeOrder.getPrice(); // Maker price
            
            executeTrade(limitOrder, oppositeOrder, matchQty, matchPrice);
            
            remainingQty = remainingQty.subtract(matchQty);
        }
        
        // Nếu còn lại, thêm vào order book
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            limitOrder.setQuantity(remainingQty);
            limitOrder.setStatus(
                remainingQty.compareTo(limitOrder.getQuantity()) < 0 
                    ? OrderStatus.PARTIALLY_FILLED 
                    : OrderStatus.PENDING
            );
            orderRepository.save(limitOrder);
        }
    }
    
    /**
     * Tìm lệnh có thể khớp
     */
    private List<FuturesOrder> findMatchableOrders(
            String symbol, 
            OrderSide side, 
            BigDecimal price, 
            OrderSide originalSide) {
        
        Sort sort = side == OrderSide.SELL 
            ? Sort.by("price").ascending().and(Sort.by("createdAt"))
            : Sort.by("price").descending().and(Sort.by("createdAt"));
        
        List<FuturesOrder> orders = orderRepository
            .findBySymbolAndSideAndStatusAndType(
                symbol, side, OrderStatus.PENDING, OrderType.LIMIT, sort
            );
        
        // Lọc theo điều kiện giá
        return orders.stream()
            .filter(o -> {
                if (originalSide == OrderSide.BUY) {
                    // Buy limit chỉ khớp với Sell có giá <= buy price
                    return o.getPrice().compareTo(price) <= 0;
                } else {
                    // Sell limit chỉ khớp với Buy có giá >= sell price
                    return o.getPrice().compareTo(price) >= 0;
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Thực hiện giao dịch
     */
    private void executeTrade(
            FuturesOrder takerOrder,
            FuturesOrder makerOrder,
            BigDecimal quantity,
            BigDecimal price) {
        
        // Cập nhật maker order
        BigDecimal newMakerQty = makerOrder.getQuantity().subtract(quantity);
        if (newMakerQty.compareTo(BigDecimal.ZERO) == 0) {
            makerOrder.setStatus(OrderStatus.FILLED);
        } else {
            makerOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            makerOrder.setQuantity(newMakerQty);
        }
        orderRepository.save(makerOrder);
        
        // Thực hiện cho cả hai bên
        tradingService.executeOrder(takerOrder, price);
        tradingService.executeOrder(makerOrder, price);
        
        // Log trade
        System.out.println(String.format(
            "✅ TRADE: %s %s %.8f @ %.2f",
            takerOrder.getSymbol(),
            takerOrder.getSide(),
            quantity,
            price
        ));
    }
}
```

---

## API Order Book

### Endpoint
```
GET /api/v1/futures/orderbook/{symbol}
```

### Request Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---------|------|----------|-------|
| `symbol` | String | ✅ | Cặp giao dịch (VD: BTCUSDT) |
| `limit` | Integer | ❌ | Số mức giá mỗi bên (mặc định: 20, tối đa: 100) |

### Response Format

```json
{
  "symbol": "BTCUSDT",
  "lastUpdateId": 1701432000000,
  "bids": [
    ["44950.00", "0.500"],
    ["44940.00", "1.200"],
    ["44930.00", "0.800"]
  ],
  "asks": [
    ["45000.00", "0.300"],
    ["45010.00", "0.700"],
    ["45020.00", "1.500"]
  ],
  "spread": {
    "absolute": 50.00,
    "percentage": 0.11
  },
  "depth": {
    "bidVolume": 2.500,
    "askVolume": 2.500,
    "totalVolume": 5.000
  }
}
```

### Response Fields

| Field | Kiểu | Mô tả |
|-------|------|-------|
| `symbol` | String | Cặp giao dịch |
| `lastUpdateId` | Long | Timestamp cập nhật lần cuối |
| `bids` | Array | Danh sách lệnh mua [price, quantity] |
| `asks` | Array | Danh sách lệnh bán [price, quantity] |
| `spread.absolute` | BigDecimal | Chênh lệch giá tuyệt đối |
| `spread.percentage` | BigDecimal | Chênh lệch giá % |
| `depth.bidVolume` | BigDecimal | Tổng khối lượng lệnh mua |
| `depth.askVolume` | BigDecimal | Tổng khối lượng lệnh bán |

### Implementation

```java
@RestController
@RequestMapping("/api/v1/futures")
public class FuturesOrderBookController {
    
    @Autowired
    private FuturesOrderBookService orderBookService;
    
    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<?> getOrderBook(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            if (limit > 100) limit = 100;
            
            var orderBook = orderBookService.getOrderBook(symbol, limit);
            return ResponseEntity.ok(orderBook);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }
}
```

---

## WebSocket Real-time Updates

### Kết Nối WebSocket

```javascript
const ws = new WebSocket('wss://api.example.com/ws/futures/orderbook');

ws.onopen = () => {
  // Subscribe to order book updates
  ws.send(JSON.stringify({
    method: 'SUBSCRIBE',
    params: ['btcusdt@depth'],
    id: 1
  }));
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Order Book Update:', data);
  updateOrderBookUI(data);
};
```

### Update Message Format

```json
{
  "e": "depthUpdate",
  "E": 1701432000000,
  "s": "BTCUSDT",
  "U": 157,
  "u": 160,
  "b": [
    ["44950.00", "0.500"],
    ["44940.00", "1.200"]
  ],
  "a": [
    ["45000.00", "0.300"],
    ["45010.00", "0.700"]
  ]
}
```

### Fields

| Field | Mô tả |
|-------|-------|
| `e` | Event type: "depthUpdate" |
| `E` | Event time |
| `s` | Symbol |
| `U` | First update ID |
| `u` | Final update ID |
| `b` | Bids to update |
| `a` | Asks to update |

### Client-side Update Logic

```javascript
class OrderBookManager {
  constructor() {
    this.bids = new Map(); // price -> quantity
    this.asks = new Map();
  }
  
  handleUpdate(update) {
    // Update bids
    update.b.forEach(([price, qty]) => {
      if (parseFloat(qty) === 0) {
        this.bids.delete(price);
      } else {
        this.bids.set(price, qty);
      }
    });
    
    // Update asks
    update.a.forEach(([price, qty]) => {
      if (parseFloat(qty) === 0) {
        this.asks.delete(price);
      } else {
        this.asks.set(price, qty);
      }
    });
    
    this.render();
  }
  
  render() {
    // Sort and display
    const sortedBids = Array.from(this.bids.entries())
      .sort((a, b) => parseFloat(b[0]) - parseFloat(a[0]))
      .slice(0, 20);
    
    const sortedAsks = Array.from(this.asks.entries())
      .sort((a, b) => parseFloat(a[0]) - parseFloat(b[0]))
      .slice(0, 20);
    
    // Update UI
    updateBidsUI(sortedBids);
    updateAsksUI(sortedAsks);
  }
}
```

---

## Ví Dụ Thực Tế

### 1. Lấy Order Book

```bash
curl -X GET "https://api.example.com/api/v1/futures/orderbook/BTCUSDT?limit=10"
```

**Response:**
```json
{
  "symbol": "BTCUSDT",
  "lastUpdateId": 1701432000000,
  "bids": [
    ["44950.00", "0.500"],
    ["44940.00", "1.200"],
    ["44930.00", "0.800"],
    ["44920.00", "2.100"],
    ["44910.00", "0.600"]
  ],
  "asks": [
    ["45000.00", "0.300"],
    ["45010.00", "0.700"],
    ["45020.00", "1.500"],
    ["45030.00", "0.900"],
    ["45040.00", "1.100"]
  ],
  "spread": {
    "absolute": 50.00,
    "percentage": 0.11
  },
  "depth": {
    "bidVolume": 5.200,
    "askVolume": 4.500,
    "totalVolume": 9.700
  }
}
```

### 2. Phân Tích Market Depth

```python
import requests

def analyze_market_depth(symbol, limit=50):
    url = f"https://api.example.com/api/v1/futures/orderbook/{symbol}"
    params = {"limit": limit}
    
    response = requests.get(url, params=params)
    data = response.json()
    
    # Tính tổng volume theo khoảng giá
    best_bid = float(data['bids'][0][0])
    best_ask = float(data['asks'][0][0])
    
    # Volume trong 1% spread
    bid_volume_1pct = sum(
        float(qty) for price, qty in data['bids']
        if float(price) >= best_bid * 0.99
    )
    
    ask_volume_1pct = sum(
        float(qty) for price, qty in data['asks']
        if float(price) <= best_ask * 1.01
    )
    
    print(f"Symbol: {symbol}")
    print(f"Best Bid: {best_bid}, Best Ask: {best_ask}")
    print(f"Spread: {data['spread']['percentage']}%")
    print(f"Bid Volume (1%): {bid_volume_1pct} BTC")
    print(f"Ask Volume (1%): {ask_volume_1pct} BTC")
    
    # Đánh giá thanh khoản
    if bid_volume_1pct > 10 and ask_volume_1pct > 10:
        print("✅ High Liquidity")
    elif bid_volume_1pct > 5 and ask_volume_1pct > 5:
        print("⚠️ Medium Liquidity")
    else:
        print("❌ Low Liquidity")

analyze_market_depth("BTCUSDT")
```

### 3. Tính Slippage Ước Tính

```javascript
function estimateSlippage(orderBook, side, quantity) {
  const orders = side === 'BUY' ? orderBook.asks : orderBook.bids;
  
  let remainingQty = quantity;
  let totalCost = 0;
  let filledQty = 0;
  
  for (const [price, qty] of orders) {
    const priceNum = parseFloat(price);
    const qtyNum = parseFloat(qty);
    
    const fillQty = Math.min(remainingQty, qtyNum);
    totalCost += fillQty * priceNum;
    filledQty += fillQty;
    remainingQty -= fillQty;
    
    if (remainingQty <= 0) break;
  }
  
  if (remainingQty > 0) {
    return { error: 'Insufficient liquidity' };
  }
  
  const avgPrice = totalCost / filledQty;
  const bestPrice = parseFloat(orders[0][0]);
  const slippage = ((avgPrice - bestPrice) / bestPrice) * 100;
  
  return {
    averagePrice: avgPrice,
    bestPrice: bestPrice,
    slippage: slippage.toFixed(4) + '%',
    totalCost: totalCost
  };
}

// Ví dụ: Mua 5 BTC
const result = estimateSlippage(orderBook, 'BUY', 5);
console.log(result);
// Output: {
//   averagePrice: 45015.60,
//   bestPrice: 45000.00,
//   slippage: '0.0347%',
//   totalCost: 225078.00
// }
```

---

## Tối Ưu Hóa

### 1. Caching với Redis

```java
@Service
public class OrderBookCacheService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String ORDER_BOOK_KEY = "orderbook:";
    
    public void cacheOrderBook(String symbol, Map<String, Object> orderBook) {
        String key = ORDER_BOOK_KEY + symbol;
        String json = new ObjectMapper().writeValueAsString(orderBook);
        redisTemplate.opsForValue().set(key, json, 1, TimeUnit.SECONDS);
    }
    
    public Optional<Map<String, Object>> getCachedOrderBook(String symbol) {
        String key = ORDER_BOOK_KEY + symbol;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            return Optional.of(new ObjectMapper().readValue(json, Map.class));
        }
        return Optional.empty();
    }
}
```

### 2. Database Indexing

```sql
-- Index cho query order book nhanh
CREATE INDEX idx_futures_orders_symbol_side_status_price 
ON futures_orders(symbol, side, status, price DESC, created_at);

-- Index cho tìm lệnh theo user
CREATE INDEX idx_futures_orders_uid_status 
ON futures_orders(uid, status, created_at DESC);
```

---

## Tài Liệu Liên Quan

- [Futures Order API](./FUTURES_ORDER_API.md)
- [Futures Kline API](./FUTURES_KLINE_API.md)
- [WebSocket API](./WEBSOCKET_API.md) (Chưa có)

---

**Phiên bản**: 1.0  
**Cập nhật lần cuối**: 2025-12-01  
**Tác giả**: API Exchange Development Team
