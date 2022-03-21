package io.oreto.jpa.dsl.test.entities;

import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepo extends JpaSpecRepository<Person, Long> {
}
