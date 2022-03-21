package io.oreto.jpa.dsl.test.entities;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface JpaSpecRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    Page<T> query(String q, Pageable page, String... fetch);
    Optional<T> queryOne(String q, String... fetch);
    List<T> queryAll(String q, String... fetch);
}
