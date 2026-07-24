package com.powermobile.crm.domain.port.out;

public interface EventPublisher {
    void publish(String aggregateType, String aggregateId, Object payload);
}