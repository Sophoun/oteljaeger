package com.sophoun.oteljaeger.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ExternalUserService {

	@Autowired
	private RestTemplate restTemplate;

	private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

	public Map<String, Object> getUserInfo(Long userId) {
		return restTemplate.getForObject(BASE_URL + "/users/" + userId, Map.class);
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getUserPosts(Long userId) {
		Object result = restTemplate.getForObject(BASE_URL + "/posts?userId=" + userId, Object.class);
		if (result instanceof List) {
			return (List<Map<String, Object>>) result;
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getPostComments(Long postId) {
		Object result = restTemplate.getForObject(BASE_URL + "/comments?postId=" + postId, Object.class);
		if (result instanceof List) {
			return (List<Map<String, Object>>) result;
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getUserTodos(Long userId) {
		Object result = restTemplate.getForObject(BASE_URL + "/todos?userId=" + userId, Object.class);
		if (result instanceof List) {
			return (List<Map<String, Object>>) result;
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getUserAlbums(Long userId) {
		Object result = restTemplate.getForObject(BASE_URL + "/albums?userId=" + userId, Object.class);
		if (result instanceof List) {
			return (List<Map<String, Object>>) result;
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getAlbumPhotos(Long albumId) {
		Object result = restTemplate.getForObject(BASE_URL + "/photos?albumId=" + albumId, Object.class);
		if (result instanceof List) {
			return (List<Map<String, Object>>) result;
		}
		return Collections.emptyList();
	}

	public Map<String, Object> getPost(Long postId) {
		return restTemplate.getForObject(BASE_URL + "/posts/" + postId, Map.class);
	}
}
