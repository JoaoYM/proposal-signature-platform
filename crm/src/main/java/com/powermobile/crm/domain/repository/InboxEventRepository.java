package com.powermobile.crm.domain.repository;
import com.powermobile.crm.domain.model.InboxEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {
    Optional<InboxEvent> findTopByAggregateIdOrderByOccurredAtDesc(String aggregateId);
}
