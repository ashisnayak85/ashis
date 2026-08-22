package com.enterprise.ems.repository;

import com.enterprise.ems.entity.TicketConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketConversationRepository extends JpaRepository<TicketConversation, Long> {

    List<TicketConversation> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    Optional<TicketConversation> findFirstByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
