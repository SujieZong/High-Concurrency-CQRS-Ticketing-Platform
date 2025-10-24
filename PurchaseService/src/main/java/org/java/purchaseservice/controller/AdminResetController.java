package org.java.purchaseservice.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.purchaseservice.service.admin.AdminResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative endpoints to reset infrastructure components. There should be some protection
 * method since this should be admin actions.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin/reset")
public class AdminResetController {

  private final AdminResetService adminResetService;

  @PostMapping("/redis")
  public ResponseEntity<Map<String, String>> resetRedis() {
    adminResetService.resetRedis();
    return ResponseEntity.ok(Map.of("status", "Redis reset completed"));
  }

  @PostMapping("/mysql")
  public ResponseEntity<Map<String, String>> resetMySql() {
    adminResetService.resetMySql();
    return ResponseEntity.ok(Map.of("status", "MySQL ticket table reset completed"));
  }
}
