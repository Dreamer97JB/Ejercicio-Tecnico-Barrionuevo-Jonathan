package com.reto.tecnico.customer_service.messaging;

import com.reto.tecnico.customer_service.config.RabbitProperties;
import com.reto.tecnico.customer_service.entity.Customer;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerEventPublisher {

    private static final String TYPE_CREATED = "CustomerCreated";
    private static final String TYPE_UPDATED = "CustomerUpdated";
    private static final String TYPE_DEACTIVATED = "CustomerDeactivated";

    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;
    private final RabbitProperties rabbitProperties;

    public void publishCreated(Customer customer) {
        publish(customer, rabbitProperties.getRouting().getCreated(), TYPE_CREATED);
    }

    public void publishUpdated(Customer customer) {
        publish(customer, rabbitProperties.getRouting().getUpdated(), TYPE_UPDATED);
    }

    public void publishDeactivated(Customer customer) {
        publish(customer, rabbitProperties.getRouting().getDeactivated(), TYPE_DEACTIVATED);
    }

    private void publish(Customer customer, String routingKey, String eventType) {
        CustomerEventPayload payload = new CustomerEventPayload(
                customer.getClienteId(),
                customer.getIdentificacion(),
                customer.getTipoIdentificacion(),
                customer.getName(),
                customer.isActive()
        );
        CustomerEvent event = new CustomerEvent(
                UUID.randomUUID(),
                eventType,
                OffsetDateTime.now(clock),
                payload
        );
        rabbitTemplate.convertAndSend(rabbitProperties.getExchange(), routingKey, event);
    }
}
