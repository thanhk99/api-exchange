package api.exchange.services;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.dtos.Request.StaticsOrderRequest;
import api.exchange.models.TransactionAds;
import api.exchange.repository.TransactionsAdsRepository;
import api.exchange.sercurity.jwt.JwtUtil;

@Service
public class StaticsOrderService {

    @Autowired
    private TransactionsAdsService transactionsAdsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TransactionsAdsRepository transactionsAdsRepository;

    public ResponseEntity<?> getOrderNews(StaticsOrderRequest entity, String header) {
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);
        List<TransactionAds> rs = new ArrayList<>();
        LocalDateTime startDateTime = entity.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = entity.getEndDate().atTime(LocalTime.MAX);
        rs = transactionsAdsRepository.findByUidWithFilters(uid, startDateTime, endDateTime, entity.getStatus(),
                entity.getTradeType());
        return ResponseEntity.ok(Map.of("message", "success", "data", rs));
    }
}
