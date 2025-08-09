package api.exchange.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.models.coinModel;
import api.exchange.models.priceHistoryModel;
import api.exchange.services.CoinDataService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/hisPriceCoin")
public class PriceCoinHistoryController {

    @Autowired
    private CoinDataService coinService;

    @PostMapping("/getList")
    public ResponseEntity<?> getListPrice(@RequestBody coinModel entity) {
        try {
            List<priceHistoryModel> listPrice = coinService.getListHisCoin(entity);
            if (listPrice == null) {
                return ResponseEntity.badRequest().body(Map.of("ERROR", "INVALID", "message", "list is null"));
            }
            return ResponseEntity.ok(listPrice);
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.internalServerError().body(Map.of("ERROR", "SERVER ERROR"));
        }
    }

}
