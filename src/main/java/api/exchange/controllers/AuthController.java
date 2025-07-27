package api.exchange.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import api.exchange.dtos.Requset.LoginRequest;
import api.exchange.dtos.Requset.SignupRequest;
import api.exchange.dtos.Response.RefreshTokenRequest;
import api.exchange.models.UserDevice;
import api.exchange.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest, HttpServletRequest requset) {
        return authService.signup(signupRequest, requset);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest requset) {
        return authService.loginService(loginRequest, requset);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest requset) {
        return authService.RefreshTokenService(refreshTokenRequest, requset);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader, @RequestBody UserDevice entity) {
        return authService.revokeDevice(authHeader, entity);
    }

    @PostMapping("/existEmail")
    public ResponseEntity<?> isExistEmail(@RequestBody SignupRequest signupRequest) {
        return authService.isExistEmail(signupRequest);
    }

}
