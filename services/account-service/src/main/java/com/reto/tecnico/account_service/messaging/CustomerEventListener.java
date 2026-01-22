package com.reto.tecnico.account_service.messaging;

import com.reto.tecnico.account_service.service.EventProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerEventListener {

    private final EventProcessingService eventProcessingService;

    @RabbitListener(queues = "#{@rabbitProperties.queue}")
    public void onMessage(CustomerEvent event) {
        eventProcessingService.process(event);
    }
}
