package com.enterprise.ems.repository;

import com.enterprise.ems.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);
}
