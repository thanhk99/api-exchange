package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import api.exchange.dtos.Requset.PasswordRequest;
import api.exchange.services.PasswordService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("api/v1/password")
public class PasswordController {

    @Autowired
    private PasswordService passwordService;

    @PostMapping("changeLv2")
    public ResponseEntity<?> changePassLv2(@RequestHeader("Authorization") String authHeader,
            @RequestBody PasswordRequest request) {
        return passwordService.changePassLv2Service(authHeader, request);
    }

    @PostMapping("changePass")
    public ResponseEntity<?> changePass(@RequestHeader("Authorization") String authHeader,
            @RequestBody PasswordRequest request) {
        return passwordService.changePassService(authHeader, request);
    }

}
