package com.enterprise.ems.repository;

import com.enterprise.ems.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/*
 * PURPOSE: Data access layer for Role entity
 * ANNOTATION: @Repository - Spring stereotype for persistence layer
 * EXTENDS: JpaRepository provides CRUD + pagination out of the box
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
