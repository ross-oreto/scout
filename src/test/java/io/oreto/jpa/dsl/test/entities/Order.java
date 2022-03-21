package io.oreto.jpa.dsl.test.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "P_ORDER")
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Double amount;
    private Double shipping;

    @Column(updatable = false)
    private LocalDateTime purchasedOn;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.LAZY)
    private final List<Item> items;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.LAZY)
    private Person person;

    public Order() {
        items = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }
    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getShipping() {
        return shipping;
    }
    public void setShipping(Double shipping) {
        this.shipping = shipping;
    }

    public LocalDateTime getPurchasedOn() {
        return purchasedOn;
    }

    public void setPurchasedOn(LocalDateTime purchasedOn) {
        this.purchasedOn = purchasedOn;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Order withId(Long id) {
        this.id = id;
        return this;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items.clear();
        addItem(items);
    }

    public Order withAmount(Double amount) {
        this.amount = amount;
        return this;
    }

    public Order withShipping(Double shipping) {
        this.shipping = shipping;
        return this;
    }

    public Order withPurchasedOn(LocalDateTime purchasedOn) {
        this.purchasedOn = purchasedOn;
        return this;
    }

    public Order withItems(List<Item> items) {
        setItems(items);
        return this;
    }

    public Order addItem(Item... items) {
        this.items.addAll(Arrays.asList(items));
        return this;
    }

    public Order addItem(Collection<Item> items) {
        this.items.addAll(items);
        return this;
    }

    @PrePersist
    public void createdStamp() {
        setPurchasedOn(LocalDateTime.now());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), amount);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Order)) {
            return false;
        }
        Order person = (Order) o;
        return Objects.equals(person.getId(), getId())
                && Objects.equals(person.getAmount(), getAmount());
    }
}
