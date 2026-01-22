package com.reto.tecnico.account_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rabbit")
public class RabbitProperties {

    private String exchange = "customer.events";
    private String queue = "account.customer.events";
    private Routing routing = new Routing();

    @Getter
    @Setter
    public static class Routing {
        private String pattern = "customer.*";
        private String created = "customer.created";
        private String updated = "customer.updated";
        private String deactivated = "customer.deactivated";
    }
}
