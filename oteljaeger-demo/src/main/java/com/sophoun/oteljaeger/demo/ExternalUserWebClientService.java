package com.sophoun.oteljaeger.demo;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ExternalUserWebClientService {

	private final WebClient webClient;

	private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

	public ExternalUserWebClientService(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.baseUrl(BASE_URL).build();
	}

	public Map<String, Object> getUserInfo(Long userId) {
		return webClient.get()
				.uri("/users/{id}", userId)
				.retrieve()
				.bodyToMono(Map.class)
				.block();
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getUserPosts(Long userId) {
		Object result = webClient.get()
				.uri("/posts?userId={userId}", userId)
				.retrieve()
				.bodyToMono(Object.class)
				.block();
		if (result instanceof List) {
			return (List<Map<String, Object>>) result;
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getPostComments(Long postId) {
		Object result = webClient.get()
				.uri("/comments?postId={postId}", postId)
				.retrieve()
				.bodyToMono(Object.class)
				.block();
		if (result instanceof List) {
			return (List<Map<String, Object>>) result;
		}
		return Collections.emptyList();
	}
}
