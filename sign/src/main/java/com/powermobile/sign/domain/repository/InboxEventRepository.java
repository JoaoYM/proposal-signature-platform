package com.powermobile.sign.domain.repository;
import com.powermobile.sign.domain.model.InboxEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {}
