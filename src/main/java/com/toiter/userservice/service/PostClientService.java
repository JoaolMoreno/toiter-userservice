package com.toiter.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Service
public class PostClientService {

    private static final Logger logger = LoggerFactory.getLogger(PostClientService.class);
    private final RestTemplate restTemplate;

    @Value("${SERVICE_POST_URL}")
    private String postServiceUrl;

    @Value("${service.shared-key}")
    private String sharedKey;

    public PostClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Integer getPostsCount(Long userId) {
        logger.debug("Fetching posts count for user ID: {}", userId);
        String url = postServiceUrl + "/posts/count?userId=" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sharedKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, Integer.class).getBody();
    }

    public void updateProfileImage(Long userId, String imageUrl) {
        logger.debug("Updating profile image for user ID: {}", userId);
        String url = postServiceUrl + "/posts/update-profile-image?userId=" + userId + "&imageUrl=" + imageUrl;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sharedKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }
}
