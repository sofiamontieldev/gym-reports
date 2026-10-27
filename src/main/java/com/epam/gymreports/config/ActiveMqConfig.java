package com.epam.gymreports.config;

import com.epam.gymreports.messaging.TrainerWorkloadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

@Configuration
@ConditionalOnProperty(
        name = "gym.integrations.activemq.enabled",
        havingValue = "true")
public class ActiveMqConfig {

    public static final String EVENT_TYPE_ID = "trainer-workload-v1";

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {

        MappingJackson2MessageConverter converter =
                new MappingJackson2MessageConverter();

        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(Map.of(
                EVENT_TYPE_ID,
                TrainerWorkloadEvent.class));

        return converter;
    }

    @Bean
    public DefaultJmsListenerContainerFactory
            jmsListenerContainerFactory(
                    ConnectionFactory connectionFactory,
                    DefaultJmsListenerContainerFactoryConfigurer configurer,
                    MessageConverter messageConverter) {

        DefaultJmsListenerContainerFactory factory =
                new DefaultJmsListenerContainerFactory();

        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setSessionTransacted(true);
        factory.setConcurrency("1");

        return factory;
    }
}