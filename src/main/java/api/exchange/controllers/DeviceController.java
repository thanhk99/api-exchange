package api.exchange.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.exchange.services.DeviceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("api/v1/device")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @GetMapping("/listDevice")
    public ResponseEntity<?> getListDevice(@RequestHeader("Authorization") String authHeader) {
        return deviceService.getListDevice(authHeader);
    }

}
