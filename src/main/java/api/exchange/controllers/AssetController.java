package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.sercurity.jwt.JwtUtil;
import api.exchange.services.AssetService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AssetService assetService;

    @GetMapping("/overview")
    public ResponseEntity<?> getAssetOverview(@RequestHeader("Authorization") String header) {
        String jwt = header.substring(7);
        String uid = jwtUtil.getUserIdFromToken(jwt);
        if (uid == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid token"));
        }
        try {
            Map<String, Object> data = assetService.getAssetOverview(uid);
            return ResponseEntity.ok(Map.of("message", "success", "data", data));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "SERVER_ERROR", "error", e.getMessage()));
        }
    }
}
