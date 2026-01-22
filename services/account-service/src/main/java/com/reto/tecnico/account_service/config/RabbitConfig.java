package com.reto.tecnico.account_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange customerExchange(RabbitProperties rabbitProperties) {
        return new TopicExchange(rabbitProperties.getExchange());
    }

    @Bean
    public Queue customerQueue(RabbitProperties rabbitProperties) {
        return new Queue(rabbitProperties.getQueue());
    }

    @Bean
    public Binding customerBinding(Queue customerQueue, TopicExchange customerExchange, RabbitProperties rabbitProperties) {
        return BindingBuilder.bind(customerQueue)
                .to(customerExchange)
                .with(rabbitProperties.getRouting().getPattern());
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
