package api.exchange.sercurity.jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import api.exchange.models.TokenBlacklist;
import api.exchange.models.User;
import api.exchange.models.refreshToken;
import api.exchange.repository.RefreshTokenRepository;
import api.exchange.repository.TokenBlacklistRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final JwtParser jwtParser;

    @Value("${jwt.accessToken.expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refreshToken.expiration}")
    private long refreshTokenExpirationDay;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        try {
            // Kiểm tra secret key không null hoặc rỗng
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalArgumentException("JWT secret key must not be null or empty");
            }

            byte[] keyBytes = Decoders.BASE64.decode(secret);

            // Kiểm tra độ dài key
            if (keyBytes.length < 32) { // 256-bit
                throw new IllegalArgumentException("JWT secret key must be at least 256-bit (32 bytes)");
            }

            this.key = Keys.hmacShaKeyFor(keyBytes);
            this.jwtParser = Jwts.parser()
                    .verifyWith(key) // Sử dụng verifyWith thay vì setSigningKey
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JwtUtil: " + e.getMessage(), e);
        }
    }

    // Tạo access token
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        claims.put("user-id", user.getUid());
        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // Tạo refresh token và lưu vào database
    public refreshToken generateRefreshToken(User user, String deviceId) {
        // Xóa token cũ nếu tồn tại
        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);
        LocalDateTime timeNow = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        // Tạo token mới
        refreshToken newToken = refreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .deviceId(deviceId)
                .lastUsedAt(timeNow)
                .expiresAt(timeNow.plusDays(refreshTokenExpirationDay))
                .build();

        return refreshTokenRepository.save(newToken);
    }

    // Xác thực token
    public boolean validateToken(String token) {
        try {
            // Kiểm tra trong blacklist
            if (tokenBlacklistRepository.existsByToken(token)) {
                return false;
            }

            jwtParser.parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException
                | IllegalArgumentException e) {
            return false;
        }
    }

    // Lấy username từ token
    public String getUsernameFromToken(String token) {
        return jwtParser.parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Thêm token vào blacklist
    public void addToBlacklist(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        TokenBlacklist blacklistedToken = TokenBlacklist.builder()
                .token(token)
                .blacklistedAt(Instant.now())
                .expiresAt(claims.getExpiration().toInstant())
                .build();

        tokenBlacklistRepository.save(blacklistedToken);
    }

    public String getUserIdFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        Object userIdValue = claims.get("user-id");

        if (userIdValue == null) {
            throw new IllegalArgumentException("Token does not contain user-id");
        } else {
            return userIdValue.toString();
        }
    }
}