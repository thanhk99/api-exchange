package api.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper redisObjectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Enable default typing for GenericJackson2JsonRedisSerializer equivalent
        // behavior if needed,
        // but GenericJackson2JsonRedisSerializer handles typing itself if passed no
        // mapper?
        // Actually, GenericJackson2JsonRedisSerializer(ObjectMapper) constructor
        // exists.
        // It's safer to just configure the serializer with the mapper.
        return objectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        com.fasterxml.jackson.databind.ObjectMapper mapper = redisObjectMapper();
        // GenericJackson2JsonRedisSerializer deals with storing type info which is
        // important for Redis.
        // We can pass the mapper to it.
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, api.exchange.models.OrderBooks> redisTemplateOrderBooks(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, api.exchange.models.OrderBooks> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        com.fasterxml.jackson.databind.ObjectMapper mapper = redisObjectMapper();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }
}