package com.venueconnect.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String BOOKING_EXCHANGE_NAME = "booking.exchange";
    public static final String BOOKING_DLX_NAME = "booking.dlx"; // Dead Letter Exchange

    // Queue names
    public static final String BOOKING_QUEUE_NAME = "booking.queue";
    public static final String BOOKING_DLQ_NAME = "booking.dlq"; // Dead Letter Queue

    // Routing keys
    public static final String BOOKING_ROUTING_KEY = "booking.key";
    public static final String BOOKING_DLQ_ROUTING_KEY = "booking.dlq.key";

    // --- Exchanges ---
    @Bean
    DirectExchange bookingExchange() {
        return new DirectExchange(BOOKING_EXCHANGE_NAME);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(BOOKING_DLX_NAME);
    }

    // --- Queues ---
    @Bean
    Queue bookingQueue() {
        // Configure the main queue to route failed messages to the DLX
        return QueueBuilder.durable(BOOKING_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", BOOKING_DLX_NAME)
                .withArgument("x-dead-letter-routing-key", BOOKING_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        // This queue holds messages that failed processing
        return QueueBuilder.durable(BOOKING_DLQ_NAME).build();
    }

    // --- Bindings ---
    @Bean
    Binding bookingBinding(Queue bookingQueue, DirectExchange bookingExchange) {
        return BindingBuilder.bind(bookingQueue).to(bookingExchange).with(BOOKING_ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(BOOKING_DLQ_ROUTING_KEY);
    }

    // --- Message Converter ---
    // Configure RabbitMQ to use JSON for message bodies
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // --- RabbitTemplate Configuration ---
    // Configure the RabbitTemplate to use our JSON converter
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}