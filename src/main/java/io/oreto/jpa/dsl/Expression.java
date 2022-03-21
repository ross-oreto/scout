package io.oreto.jpa.dsl;

import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Expression<T> {
    static final String METHOD_REF = "::";
    static final String COLLECTOR_REF = METHOD_REF + Operator.collect;

    static final String NOT = "not_";

    public enum Operator {
        eq, isnull, gt, lt, gte, lte, in
        , contains, icontains, startswith, istartswith, endswith, iendswith
        , collect(true);

        public static boolean isValid(String s) {
            for (Operator operator : values()) {
                if (operator.name().equals(s))
                    return true;
            }
            return false;
        }

        private final boolean aggregate;

        Operator(boolean aggregate) {
            this.aggregate = aggregate;
        }
        Operator() {
            this.aggregate = false;
        }

        public boolean isAggregate() {
            return aggregate;
        }
    }

    static Path<?> toPath(Path<?> root, String key) {
        Path<?> path = root;
        String field = null;
        if (Str.isEmpty(key))
            return path;
        if (key.contains(".")) {
            String[] fields = key.split("\\.");
            key = fields[0];
            field = fields.length > 1 ? fields[1] : null;
        }
        path = path.get(key);
        String name = path.getClass().getName();
        if (name.contains("SingularAttribute")) {
            return field == null ? path : path.get(field);
        }
        if (path.getJavaType() == Map.class) {
            path = root instanceof Root ? ((Root<?>)root).joinMap(key) : ((Join<?,?>)root).joinMap(key);
            if ("key".equals(field)) {
                path = ((MapJoin<?, ?, ?>) path).key();
            } else if ("value".equals(field)) {
                path = ((MapJoin<?, ?, ?>) path).value();
            }
        } else if (path.getJavaType() == List.class) {
            path = root instanceof Root ? ((Root<?>)root).joinList(key) : ((Join<?,?>)root).joinList(key);
        }
        return path;
    }

    public enum Function {
        count(true)
        , avg(true)
        , sum(true)
        , max(true)
        , min(true)
        , greatest(true)
        , least(true)
        , count_distinct(true);

        private final boolean aggregate;

        Function(boolean aggregate) {
            this.aggregate = aggregate;
        }

        public static boolean isValid(String s) {
            for (Function f : values()) {
                if (f.name().equals(s))
                    return true;
            }
            return false;
        }

        public boolean isAggregate() {
            return aggregate;
        }
    }

    String key;
    String s;
    Object value;
    Operator operator;
    boolean negate;
    boolean prop;
    boolean parent;
    Function f1;
    Function f2;
    Path<?> p1;
    Path<?> p2;

    Expression(String expr) {
        this(expr, null);
    }

    Expression(String expr, List<Object> values) {
        String[] parts = expr.split(":", 2);
        key = parts[0].trim();

        String accessor = key + METHOD_REF;
        if (expr.startsWith(accessor)) {
            String op = expr.substring(expr.indexOf(accessor) + accessor.length())
                    .split(":", 2)[0].trim();
            String rawOp = op;

            if (op.startsWith(NOT)) {
                negate = true;
                op = op.substring(op.indexOf(NOT) + NOT.length());
            }
            if (!Operator.isValid(op)) {
                throw new BadQueryException("Unexpected operator: " + op);
            }

            operator = Operator.valueOf(op);
            if (Objects.nonNull(values)) {
                value = values;
            } else {
                String accessorOp = accessor + rawOp;
                String val = expr.substring(expr.indexOf(accessorOp) + (accessorOp).length()).trim();
                value = val.startsWith(":") ? val.substring(1) : "";
            }
        } else {
            operator = Operator.eq;
            value = values == null ? parts.length > 1 ? parts[1] : true : values;
        }
        s = value.toString();

        String funcRegex = "^[a-zA-Z_0-9]+\\(.*\\)$";
        // check for a left function
        if (key.matches(funcRegex)) {
            String func = key.substring(0, key.indexOf('('));
            f1 = toFunction(func);
            key = key.substring(key.indexOf('(') + 1, key.indexOf(')'));
        }
        // check for a quote string literal
        if (s.matches("^'.*'$")) {
            value = s.substring(1, s.length() - 1);
            s = value.toString();
        }
        // check for a function
        else if (s.matches(funcRegex)) {
            String func = s.substring(0, s.indexOf('('));
            f2 = toFunction(func);
            s = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
        }
        if (s.startsWith("@")) {
            // value is a non-literal and instead a field.
            prop = true;
            value = s.substring(1);
            s = value.toString();
        } else if (s.startsWith("^")) {
            // value is a non-literal and instead a parent query field.
            prop = true;
            parent = true;
            value = s.substring(1);
            s = value.toString();
        }
    }

    protected Function toFunction(String func) {
        if (Function.isValid(func))
            return Function.valueOf(func);
        else
            throw new BadQueryException(String.format("%s is not a valid function", func));
    }

    protected boolean isAggregate() {
        boolean f1Aggregate = Objects.nonNull(f1) && f1.isAggregate();
        boolean f2Aggregate = Objects.nonNull(f2) && f2.isAggregate();
        return f1Aggregate || f2Aggregate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Predicate apply(DSL<T> dsl) {
        Path<?> root = dsl.root;
        CriteriaBuilder cb = dsl.criteriaBuilder;
        Predicate predicate;
        try {
            p1 = toPath(root, key);
            if (!prop) setValue(p1);
            if (prop) p2 = toPath(parent ? dsl.parent : root, s);
            else p2 = null;

            javax.persistence.criteria.Expression exp1 = f1 == null ? p1 : applyFunction(f1, p1, cb);
            javax.persistence.criteria.Expression exp2 = f2 == null ? p2 : applyFunction(f2, p2, cb);

            switch (operator) {
                case eq:
                    predicate = prop
                            ? cb.equal(exp1, exp2)
                            : cb.equal(exp1, value);
                    break;
                case lt:
                    if (prop)
                        predicate = cb.lt(exp1, exp2);
                    else
                        predicate = value instanceof Comparable
                                ? cb.lessThan(exp1, (Comparable) value)
                                : cb.lessThan(exp1, s);
                    break;
                case lte:
                    if (prop)
                        predicate = cb.lessThanOrEqualTo(exp1, exp2);
                    else
                        predicate = value instanceof Comparable
                                ? cb.lessThanOrEqualTo(exp1, (Comparable) value)
                                : cb.lessThanOrEqualTo(exp1, s);
                    break;
                case gt:
                    if (prop)
                        predicate = cb.gt(exp1, exp2);
                    else
                        predicate = value instanceof Comparable
                                ? cb.greaterThan(exp1, (Comparable) value)
                                : cb.greaterThan(exp1, s);
                    break;
                case gte:
                    if (prop)
                        predicate = cb.greaterThanOrEqualTo(exp1, exp2);
                    else
                        predicate = value instanceof Comparable
                                ? cb.greaterThanOrEqualTo(exp1, (Comparable) value)
                                : cb.greaterThanOrEqualTo(exp1, s);
                    break;
                case isnull:
                    predicate = cb.isNull(exp1);
                    break;
                case in:
                    predicate = exp1.in((Collection<?>) value);
                    break;
                case contains:
                    predicate = prop
                            ? cb.like(exp1, cb.concat("%", cb.concat(exp2, "%")))
                            : cb.like(exp1, String.format("%%%s%%", s));
                    break;
                case icontains:
                    predicate = prop
                            ? cb.like(cb.upper(exp1), cb.concat("%", cb.concat(cb.upper(exp2), "%")))
                            : cb.like(cb.upper(exp1), String.format("%%%s%%", s.toUpperCase()));
                    break;
                case startswith:
                    predicate = prop
                            ? cb.like(exp1, cb.concat(exp2, "%"))
                            : cb.like(exp1, String.format("%s%%", s));
                    break;
                case istartswith:
                    predicate = prop
                            ? cb.like(cb.upper(exp1), cb.concat(cb.upper(exp2), "%"))
                            : cb.like(cb.upper(exp1), String.format("%s%%", s.toUpperCase()));
                    break;
                case endswith:
                    predicate = prop
                            ? cb.like(exp1, cb.concat("%", exp2))
                            : cb.like(exp1, String.format("%%%s", s));
                    break;
                case iendswith:
                    predicate = prop
                            ? cb.like(cb.upper(exp1), cb.concat("%", cb.upper(exp2)))
                            : cb.like(cb.upper(exp1), String.format("%%%s", s.toUpperCase()));
                    break;
                default:
                    throw new BadQueryException("Unexpected operator: " + operator.name());
            }
        } catch (IllegalArgumentException e) {
            throw new BadQueryException("Invalid attribute: " + e.getMessage());
        } catch (IllegalStateException e) {
            throw new BadQueryException("Invalid path: " + e.getMessage());
        }
        return negate ? cb.not(predicate) : predicate;
    }

    protected void setValue(Path<?> path) {
        if (value instanceof Collection)
            return;
        if (Str.EMPTY.equals(s))
            value = null;
        else if (path.getJavaType() == Boolean.class) {
            value = Boolean.valueOf(s);
        } else if (path.getJavaType() == LocalDate.class) {
            value = LocalDate.parse(s);
        } else if (path.getJavaType() == LocalDateTime.class) {
            value = LocalDateTime.parse(s);
        } else if (path.getJavaType() == Date.class || path.getJavaType() == Timestamp.class) {
            try {
                value = SimpleDateFormat.getTimeInstance().parse(s);
            } catch (ParseException e) {
                throw new BadQueryException(String.format("Bad date format %s: %s", value, e.getMessage()));
            }
        } else if (path.getJavaType() == Long.class) {
            value = Str.toLong(s)
                    .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
        } else if (path.getJavaType() == Integer.class) {
            value = Str.toInteger(s)
                    .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
        } else if (path.getJavaType() == Short.class) {
            value = Str.toShort(s)
                    .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
        } else if (path.getJavaType() == Float.class) {
            value = Str.toFloat(s)
                    .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
        } else if (path.getJavaType() == Byte.class) {
            value = Str.toByte(s)
                    .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));;
        } else if (path.getJavaType() == Double.class) {
            value = Str.toDouble(s)
                    .orElseThrow(() -> new BadQueryException(String.format("%s:%s is not a number", key, value)));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected javax.persistence.criteria.Expression<?> applyFunction(Function function, Path path, CriteriaBuilder cb) {
        if (function == Function.count) {
            return cb.count(path);
        } else if (function == Function.avg) {
            return cb.avg(path);
        } else if (function == Function.sum) {
            return cb.sum(path);
        } else if (function == Function.max) {
            return cb.max(path);
        } else if (function == Function.min) {
            return cb.min(path);
        } else if (function == Function.greatest) {
            return cb.greatest(path);
        } else if (function == Function.least) {
            return cb.least(path);
        } else if (function == Function.count_distinct) {
            return cb.countDistinct(path);
        }
        return cb.count(path);
    }
}
