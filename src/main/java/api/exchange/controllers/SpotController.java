package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.models.OrderBooks;
import api.exchange.services.SpotService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("api/v1/spot")
public class SpotController {

    @Autowired
    private SpotService spotService;

    /**
     * Tạo lệnh mới (RESTful)
     */
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody OrderBooks entity,
            @RequestHeader("Authorization") String header) {
        return spotService.createOrder(entity, header);
    }

    /**
     * Hủy lệnh (RESTful)
     */
    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        return spotService.cancelOrder(orderId);
    }

}
