package io.oreto.jpa.dsl.test.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table
public class Address implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String line;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public Address withId(Long id) {
        this.id = id;
        return this;
    }

    public Address withLine(String line) {
        this.line = line;
        return this;
    }

    @Override
    public String toString() {
        return getLine();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), line);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Address)) {
            return false;
        }
        Address person = (Address) o;
        return Objects.equals(person.getId(), getId())
                && Objects.equals(person.getLine(), getLine());
    }
}
