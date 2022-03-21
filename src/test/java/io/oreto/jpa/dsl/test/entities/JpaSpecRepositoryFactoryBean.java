package io.oreto.jpa.dsl.test.entities;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;
import java.io.Serializable;

public class JpaSpecRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends JpaRepositoryFactoryBean<T, S, ID> {


    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new JpaSpecRepositoryFactory<T, ID>(entityManager);
    }

    /**
     * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public JpaSpecRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    private static class JpaSpecRepositoryFactory<T, I extends Serializable> extends JpaRepositoryFactory {
        /**
         * Creates a new {@link JpaRepositoryFactory}.
         *
         * @param entityManager must not be {@literal null}
         */
        public JpaSpecRepositoryFactory(EntityManager entityManager) {
            super(entityManager);
        }

        @Override
        protected JpaRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information
                , EntityManager entityManager) {
            @SuppressWarnings("unchecked")
            JpaEntityInformation<T, ?> entityInformation =
                    (JpaEntityInformation<T, ?>) getEntityInformation(information.getDomainType());
            return new SpecRepo<T, I>(entityInformation, entityManager);
        }

        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            return SpecRepo.class;
        }
    }
}
