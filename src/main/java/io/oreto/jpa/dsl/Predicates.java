package io.oreto.jpa.dsl;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

class Predicates {
    Predicate where;
    Predicate having;
    List<Expression<?>> grouping = new ArrayList<>();

    Expression<?>[] groupBy() {
        return grouping.stream().distinct().toArray(Expression[]::new);
    }
}
