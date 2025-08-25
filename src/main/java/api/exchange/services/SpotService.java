package api.exchange.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import api.exchange.models.OrderBooks;
import api.exchange.models.OrderBooks.OrderStatus;
import api.exchange.repository.OrderBooksRepository;
import api.exchange.sercurity.jwt.JwtUtil;
import jakarta.transaction.Transactional;

@Service
public class SpotService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderBooksRepository orderBooksRepository;

    @Autowired 
    private JwtUtil jwtUtil;

    @Transactional
    public ResponseEntity<?> createOrder(OrderBooks entity , String header){
        String jwt= header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);

        LocalDateTime createAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));


        entity.setStatus(OrderStatus.PENDING);
        entity.setUid(uid);
        entity.setCreatedAt(createAt);

        orderBooksRepository.save(entity);

        eventPublisher.publishEvent(new OrderMatchService.OrderCreatedEvent(entity));

        return ResponseEntity.ok(Map.of("message","success", "data","Tạo Order thành công "));
    }

    @Transactional 
    public ResponseEntity<?> cancleOrder(Long orderId){
        OrderBooks orderBooks = orderBooksRepository.findById(orderId).get();
        
        if (orderBooks == null){
            return ResponseEntity.badRequest().body(Map.of("message","Bad Request","data","Không tìm thấy id"));
        }
        if( orderBooks.getStatus() == OrderStatus.PENDING){
            return ResponseEntity.ok(Map.of("message","success","data","Huỷ lệnh thành công"));
        }
        return ResponseEntity.badRequest().body(Map.of("message","Bad Request","data","Không thể huỷ lệnh "));
    }
}
 