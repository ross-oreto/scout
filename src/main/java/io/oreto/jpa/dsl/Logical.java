package io.oreto.jpa.dsl;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.util.Stack;

public class Logical {
    public enum Operator {
        and, or, not
    }

    public static boolean isValid(String op) {
        for (Operator operator : Operator.values()) {
            if (operator.name().equals(op)) {
                return true;
            }
        }
        return false;
    }

    static Predicate apply(Operator operator, CriteriaBuilder builder, Predicate... predicates) {
        Predicate predicate = null;
        switch (operator) {
            case and:
                predicate = builder.and(predicates);
                break;
            case or:
                predicate = builder.or(predicates);
                break;
            case not:
                if (predicates.length > 0)
                    predicate = builder.not(predicates[0]);
                break;
            default:
                throw new BadQueryException("Unexpected logical operator: " + operator.name());
        }
        return predicate;
    }

    static Predicate apply(String operator, CriteriaBuilder builder, Predicate... predicates) {
        return apply(Operator.valueOf(operator), builder, predicates);
    }

    static Predicate apply(Predicate predicate, Stack<String> operator, CriteriaBuilder builder, Predicate p) {
        return operator.isEmpty() || predicate == null
                ? p
                : Logical.apply(operator.pop(), builder, predicate, p);
    }
}
