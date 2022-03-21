package io.oreto.jpa.dsl;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class Predicates {
    public Predicate where;
    public Predicate having;
    public List<Expression<?>> grouping = new ArrayList<>();

    public Expression<?>[] groupBy() {
        return grouping.stream().distinct().toArray(Expression[]::new);
    }
}
