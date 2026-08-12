package com.sergio.planix.team;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}
