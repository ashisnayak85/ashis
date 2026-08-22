package com.enterprise.ems.repository;

import com.enterprise.ems.entity.TicketReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketReadStatusRepository extends JpaRepository<TicketReadStatus, Long> {

    Optional<TicketReadStatus> findByTicketIdAndEmployeeId(Long ticketId, Long employeeId);
}
