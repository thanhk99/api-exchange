package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.dtos.Requset.StaticsOrderRequest;
import api.exchange.services.StaticsOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@RestController
@RequestMapping("/api/v1/statics")
public class StaticsOrderController {
    
    @Autowired
    private StaticsOrderService staticsOrderService;

    @PostMapping("/orderNews")
    public ResponseEntity<?> orderNews(@RequestBody StaticsOrderRequest entity , @RequestHeader("Authorization") String header) {     
        return staticsOrderService.getOrderNews(entity,header);
    }
    
}
