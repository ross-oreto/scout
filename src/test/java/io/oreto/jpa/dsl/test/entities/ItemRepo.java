package io.oreto.jpa.dsl.test.entities;

import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepo extends JpaSpecRepository<Item, Long> {
}
