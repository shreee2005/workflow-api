package com.workflow.demo.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/debug")
public class HeaderDebugController {

    @GetMapping("/headers")
    public ResponseEntity<?> headers(HttpServletRequest req) {
        Map<String,String> headers = new HashMap<>();
        Enumeration<String> names = req.getHeaderNames();
        while (names.hasMoreElements()) {
            String n = names.nextElement();
            headers.put(n, req.getHeader(n));
        }
        return ResponseEntity.ok(headers);
    }
}
