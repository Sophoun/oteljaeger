package com.sophoun.oteljaeger.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserPipelineService {

	@Autowired
	private ExternalUserService externalUserService;

	public Map<String, Object> executePipeline(Long userId) {
		Map<String, Object> result = new HashMap<>();

		Map<String, Object> userInfo = externalUserService.getUserInfo(userId);
		result.put("userInfo", userInfo);

		List<Map<String, Object>> posts = externalUserService.getUserPosts(userId);
		result.put("posts", posts);

		Long firstPostId = posts.isEmpty() ? 1L : ((Number) posts.get(0).get("id")).longValue();
		List<Map<String, Object>> comments = externalUserService.getPostComments(firstPostId);
		result.put("comments", comments);

		List<Map<String, Object>> todos = externalUserService.getUserTodos(userId);
		result.put("todos", todos);

		List<Map<String, Object>> albums = externalUserService.getUserAlbums(userId);
		result.put("albums", albums);

		Long firstAlbumId = albums.isEmpty() ? 1L : ((Number) albums.get(0).get("id")).longValue();
		List<Map<String, Object>> photos = externalUserService.getAlbumPhotos(firstAlbumId);
		result.put("photos", photos);

		Map<String, Object> postDetail = externalUserService.getPost(firstPostId);
		result.put("postDetail", postDetail);

		return result;
	}
}
