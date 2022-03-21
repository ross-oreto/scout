package io.oreto.jpa.dsl.test.entities;

import io.oreto.jpa.dsl.DSL;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class SpecRepo<T, ID> extends SimpleJpaRepository<T, ID> implements JpaSpecRepository<T, ID> {
    private final EntityManager entityManager;

    public SpecRepo(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public Page<T> query(String q, Pageable page, String... fetch) {
        return findAll((root, query, criteriaBuilder) -> DSL.predicate(root, query, criteriaBuilder, q, fetch), page);
    }

    @Override
    public Optional<T> queryOne(String q, String... fetch) {
        return findOne((root, query, criteriaBuilder) -> DSL.predicate(root, query, criteriaBuilder, q, fetch));
    }

    @Override
    public List<T> queryAll(String q, String... fetch) {
        return findAll((root, query, criteriaBuilder) -> DSL.predicate(root, query, criteriaBuilder, q, fetch));
    }
}
