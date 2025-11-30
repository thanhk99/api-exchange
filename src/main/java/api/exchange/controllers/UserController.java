package api.exchange.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.dtos.Request.UpdateNameRequest;
import api.exchange.dtos.Request.UpdatePhoneRequest;

import api.exchange.services.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/getProfile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        return userService.getProfileService(authHeader);
    }

    @PostMapping("/changeName")
    public ResponseEntity<?> changeName(@RequestBody UpdateNameRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return userService.changeName(request.getUsername(), authHeader);
    }

    @GetMapping("/getAllInfo")
    public ResponseEntity<?> getAllinfo(@RequestHeader("Authorization") String header) {
        return userService.getAllinfo(header);
    }

    @PostMapping("/changePhone")
    public ResponseEntity<?> changePhone(@RequestBody UpdatePhoneRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return userService.changePhone(request.getPhone(), authHeader);
    }

}
