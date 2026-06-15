package com.sophoun.oteljaeger.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

	@Autowired
	private ExternalUserService externalUserService;

	@Autowired
	private UserPipelineService userPipelineService;

	@Autowired
	private ExternalUserWebClientService externalUserWebClientService;

	@GetMapping("/user")
	public Map<String, Object> getUsername() throws InterruptedException {
		Thread.sleep(500);
		Map<String, Object> externalInfo = externalUserService.getUserInfo(1L);

		Map<String, Object> response = new HashMap<>();
		response.put("username", "johndoe");
		response.put("externalInfo", externalInfo);
		return response;
	}

	@GetMapping("/user/{id}")
	public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
		try {
			Map<String, Object> result = userPipelineService.executePipeline(id);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("error", e.getMessage());
			error.put("userId", id);
			return ResponseEntity.status(502).body(error);
		}
	}

	@GetMapping("/error-test")
	public ResponseEntity<Map<String, String>> errorTest() {
		Map<String, Object> externalInfo = externalUserService.getUserInfo(99999L);
		Map<String, String> response = new HashMap<>();
		response.put("username", "testuser");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/user-webclient/{id}")
	public ResponseEntity<Map<String, Object>> getUserByIdWebClient(@PathVariable Long id) {
		try {
			Map<String, Object> externalInfo = externalUserWebClientService.getUserInfo(id);
			Map<String, Object> response = new HashMap<>();
			response.put("userId", id);
			response.put("externalInfo", externalInfo);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("error", e.getMessage());
			error.put("userId", id);
			return ResponseEntity.status(502).body(error);
		}
	}
}
