package com.toiter.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toiter.userservice.model.UserPublicData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    /**
     * Configura um RedisTemplate para armazenar valores do tipo Long.
     *
     * @param connectionFactory a fábrica de conexões Redis
     * @return um RedisTemplate configurado para chaves do tipo String e valores do tipo Long
     */
    @Bean
    public RedisTemplate<String, Long> redisTemplateForLong(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * Configura um RedisTemplate para armazenar valores do tipo UserPublicData.
     *
     * @param connectionFactory a fábrica de conexões Redis
     * @return um RedisTemplate configurado para chaves do tipo String e valores do tipo UserPublicData
     */
    @Bean
    public RedisTemplate<String, UserPublicData> redisTemplateForUserPublicData(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, UserPublicData> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * Configura um RedisTemplate para armazenar valores do tipo User (entidade completa).
     *
     * @param connectionFactory a fábrica de conexões Redis
     * @return um RedisTemplate configurado para chaves do tipo String e valores do tipo User
     */
    @Bean
    public RedisTemplate<String, com.toiter.userservice.entity.User> redisTemplateForUser(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, com.toiter.userservice.entity.User> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());

        // Configure ObjectMapper to support Java 8 date/time types and preserve type information
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable default typing to preserve class information during serialization
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        return template;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(redisHost, redisPort);
        configuration.setPassword(RedisPassword.of(redisPassword));
        return new LettuceConnectionFactory(configuration);
    }
}
