package io.oreto.jpa.dsl.test;

import io.oreto.jpa.dsl.DSL;
import io.oreto.jpa.dsl.test.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = true)
@EnableJpaRepositories(repositoryFactoryBeanClass = JpaSpecRepositoryFactoryBean.class)
@TestPropertySource(locations = "classpath:application.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DslTests {

    @Resource
    private EntityManagerFactory entityManagerFactory;
    private EntityManager em;

    @Autowired PersonRepo personRepo;
    @Autowired OrderRepo orderRepo;
    @Autowired ItemRepo itemRepo;
    @Autowired VehicleRepo vehicleRepo;

    @BeforeEach
    public void setup() {
        if (em == null) {
            em = entityManagerFactory.createEntityManager();
            if (em.find(Person.class, 1L) == null) {
                EntityTransaction transaction = em.getTransaction();
                transaction.begin();
//                TestData.randomPeople(10, 10, 10, em);
                TestData.setupPeople(em);
                TestData.setupVehicles(em);
                em.flush();
                transaction.commit();
            }
        }
    }

    @Test
    public void listFetch() {
        assertEquals(
                em.createQuery(DSL.criteriaQuery(em, Person.class, "", "nickNames", "address")).getResultList()
                , em.createQuery(DSL.criteriaQuery(em, Person.class, null, "nickNames", "address")).getResultList()
        );
        Optional<Person> person = em.createQuery(DSL.criteriaQuery(em, Person.class, "id:1")).getResultStream().findFirst();
        assertTrue(person.isPresent());
    }

    @Test
    public void simpleAndOr() {
        assertEquals(
                1
                , personRepo.queryAll("name::icontains:ross address{ line::icontains:Nashville }").size()
        );
        assertEquals(
                2
                , personRepo.queryAll("name::icontains:ross or name::icontains:Bilbo").size()
        );
    }

    @Test
    public void queryFieldsEqual() {
        assertEquals(
                2
               , vehicleRepo.queryAll("make::eq:@make").size()
        );
    }

    @Test
    public void queryElementCollections() {
        assertEquals(
                1
                , personRepo.queryAll("nickNames:'Ross Sea'").size()
        );
        assertEquals(
                3
                , personRepo.queryAll("nickNames::in:['Doc Ock', 'The Half Blood Prince', 'Voldamort']").size()
        );
        assertEquals(
                3
                , itemRepo.queryAll("attributes:forged").size()
        );
    }

    @Test
    public void peopleWithForgedItems() {
        assertEquals(
                2
                , personRepo.queryAll("orders{ items{ attributes.value:forged } }").size()
        );
    }

    @Test
    public void peopleFromHogwarts() {
        assertEquals(
                4
                , personRepo.queryAll("address{ line::icontains:hogwarts }").size()
        );
    }

    @Test
    public void peopleFromHogwartsOrWithoutTwoItems() {
        assertEquals(
                4
                , personRepo.queryAll("address{ line::icontains:hogwarts } or orders{ count(items)::lt:2 }").size()
        );
    }

    @Test
    public void searchNames() {
        assertEquals(
                3
                , personRepo.queryAll("name::in:['Bilbo', 'Ross', 'Snape']").size()
        );
    }

    @Test
    public void findHedwig() {
        Optional<Person> person = personRepo.queryOne("orders{ items { name:'Hedwig' }}");
        assertTrue(person.isPresent());
        assertEquals("Harry Potter", person.get().getName());
    }

    @Test
    public void searchPastOrders() {
        assertEquals(personRepo.count()
                , personRepo.queryAll(String.format("orders{ purchasedOn::lt:%s }", LocalDateTime.now())).size());
    }

    @Test
    public void findOutlander() {
        assertTrue(vehicleRepo.queryOne("make:Mitsubishi and model:Outlander").isPresent());
        assertTrue(vehicleRepo.queryAll("tire { id.make:Goodyear }").size() > 0);
    }

    @Test
    public void findExpensiveOrders() {
        List<Order> orders = orderRepo.findAll();
        double averageAmount = orders.stream().mapToDouble(Order::getAmount).average().orElse(0D);
        List<Order> expensiveOrders = orders.stream()
                .filter(order -> order.getAmount() > averageAmount)
                .collect(Collectors.toList());

        List<Person> personOrders = personRepo.queryAll(String.format("orders { avg(amount)::gt:%f }}", averageAmount));

        assertEquals(expensiveOrders.size()
                , (int) personOrders.stream().map(Person::getOrders).mapToLong(Collection::size).sum());
    }

    @Test
    public void snapesOrders() {
        assertEquals(
                "Snape"
                , personRepo.queryOne("address{ line::icontains:hogwarts } and orders{ sum(amount)::gt:1100 and sum(amount)::lte:1143 }")
                        .map(Person::getName).orElse(null)
        );
        assertEquals(
                "Snape"
                , personRepo.queryOne("address{ line::icontains:hogwarts } and count(orders)::gt:1")
                        .map(Person::getName).orElse(null)
        );
    }

    @Test
    public void roomMates() {
        List<Person> people = personRepo.queryAll("address { line::collect{ count()::gt:1 } }");

        assertEquals(2, people.size());
        assertEquals("Harry Potter", people.get(0).getName());
        assertEquals("Ronald Weasley", people.get(1).getName());
    }

    @Test
    public void duplicateItems() {
        assertEquals("Ronald Weasley"
                ,personRepo.queryOne("orders{ items { name::collect{ count()::gt:1 } }}")
                        .map(Person::getName).orElse(null));
    }

    @Test
    public void duplicateOrderAmounts() {
        assertEquals(2, orderRepo.queryAll("amount::collect{ count()::gt:1 }").size());
    }

    @Test
    public void highShipping() {
        assertEquals(2, personRepo.queryAll("orders{ sum(shipping)::gt:100 }").size());
        assertEquals("Otto Octavius"
                , personRepo.queryOne("orders{ sum(shipping)::gt:sum(@amount) }").map(Person::getName).orElse(null));
    }
}
