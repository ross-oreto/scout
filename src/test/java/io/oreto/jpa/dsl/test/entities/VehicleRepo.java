package io.oreto.jpa.dsl.test.entities;

import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepo extends JpaSpecRepository<Vehicle, Long> {
}
