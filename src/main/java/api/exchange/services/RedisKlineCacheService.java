package api.exchange.services;

import api.exchange.dtos.Response.KlinesSpotResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisKlineCacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String KLINE_KEY_PREFIX = "kline:";

    public void cacheKlines(String symbol, String interval, List<KlinesSpotResponse> klines) {
        String key = KLINE_KEY_PREFIX + symbol.toUpperCase() + ":" + interval;
        try {
            String json = objectMapper.writeValueAsString(klines);
            redisTemplate.opsForValue().set(key, json, 1, TimeUnit.MINUTES); // Cache for 1 minute
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public List<KlinesSpotResponse> getCachedKlines(String symbol, String interval) {
        String key = KLINE_KEY_PREFIX + symbol.toUpperCase() + ":" + interval;
        String json = redisTemplate.opsForValue().get(key);

        if (json != null) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<KlinesSpotResponse>>() {
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
