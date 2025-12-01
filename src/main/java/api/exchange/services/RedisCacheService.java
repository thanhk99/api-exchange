package api.exchange.services;

import api.exchange.dtos.Response.KlinesFuturesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KLINE_1S_KEY_PREFIX = "futures:kline:1s:";

    /**
     * Lưu danh sách kline 1s vào Redis
     * Key: futures:kline:1s:{symbol}
     * Value: List<KlinesFuturesResponse>
     */
    public void cache1sKlines(String symbol, List<KlinesFuturesResponse> klines) {
        String key = KLINE_1S_KEY_PREFIX + symbol.toLowerCase();
        redisTemplate.opsForValue().set(key, klines, 5, TimeUnit.MINUTES); // Cache trong 5 phút
    }

    /**
     * Lấy danh sách kline 1s từ Redis
     */
    @SuppressWarnings("unchecked")
    public List<KlinesFuturesResponse> getCached1sKlines(String symbol) {
        String key = KLINE_1S_KEY_PREFIX + symbol.toLowerCase();
        return (List<KlinesFuturesResponse>) redisTemplate.opsForValue().get(key);
    }

    /**
     * Cập nhật cache khi có kline mới (đẩy vào cuối list và xóa đầu nếu quá dài)
     * Tuy nhiên để đơn giản và đồng bộ, ta có thể chỉ cache full list khi query DB
     * Hoặc invalidate cache để lần sau query DB lại.
     * Ở đây ta chọn cách: Invalidate cache khi có dữ liệu mới để đảm bảo tính nhất
     * quán đơn giản.
     */
    public void invalidate1sKlineCache(String symbol) {
        String key = KLINE_1S_KEY_PREFIX + symbol.toLowerCase();
        redisTemplate.delete(key);
    }
}
