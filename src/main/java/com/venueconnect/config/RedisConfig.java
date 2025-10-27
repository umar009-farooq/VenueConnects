package com.venueconnect.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.venueconnect.reservation.RedisExpirationListener; // <-- Import our listener
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic; // <-- Import PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter; // <-- Import Adapter
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories(basePackages = "com.venueconnect.reservation")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // --- Serializer setup (remains the same) ---
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        // --- End of Serializer setup ---

        return template;
    }

    // --- START OF UPDATED LISTENER CONFIG ---
    @Bean
    MessageListenerAdapter expirationListenerAdapter(RedisExpirationListener listener) {
        // Creates an adapter that will call the 'onMessage' method of our listener bean
        return new MessageListenerAdapter(listener, "onMessage");
    }

    @Bean
    RedisMessageListenerContainer keyExpirationListenerContainer(RedisConnectionFactory connectionFactory,
                                                                 MessageListenerAdapter expirationListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Explicitly tell the container to listen to the "__keyevent@*__:expired" channel
        // and forward messages to our adapter (which calls our listener bean)
        container.addMessageListener(expirationListenerAdapter, new PatternTopic("__keyevent@*__:expired"));

        // Optional: Error handler for the container itself
        container.setErrorHandler(e ->
                System.err.println("Error in RedisMessageListenerContainer: " + e.getMessage())); // Replace with proper logging if desired

        return container;
    }
    // --- END OF UPDATED LISTENER CONFIG ---
}