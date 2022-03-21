package io.oreto.jpa.dsl;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

public class DSL<T> {
    // Used to quote expressions
    protected static final char QUOTE = '\'';

    public static <T> Predicate predicate(Root<T> root
            , CriteriaQuery<?> query
            , CriteriaBuilder criteriaBuilder
            , String q, String...fetch) {
        return parsePredicates(predicates(new DSL<>(q, fetch(root, fetch), query, criteriaBuilder)), root, query);
    }

    public static <T> Predicate predicate(EntityManager entityManager, Class<T> tClass, String q, String...fetch) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<?> query = builder.createQuery(tClass);
        Root<T> root = query.from(tClass);
        return predicate(root, query, builder, q, fetch);
    }

    public static <T> CriteriaQuery<T> criteriaQuery(Root<T> root
            , CriteriaQuery<T> query
            , CriteriaBuilder criteriaBuilder
            , String q
            , String... fetch) {
        predicate(root, query, criteriaBuilder, q, fetch);
        return query;
    }

    public static <T> CriteriaQuery<T> criteriaQuery(EntityManager entityManager
            , Class<T> tClass
            , String q
            , String...fetch) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(tClass);
        Root<T> root = query.from(tClass);
        predicate(root, query, builder, q, fetch);
        return query;
    }

    private static <T> Root<T> fetch(Root<T> root, String... fetch) {
        for(String s : fetch) {
            root.fetch(s);
        }
        return root;
    }

    private static <T> Predicates predicates(DSL<T> dsl) {
        Predicates predicates = new Predicates();
        if (dsl.q == null || dsl.q.trim().equals("")) {
            return predicates;
        }
        for (; dsl.i < dsl.length && !dsl.shortCircuit; dsl.i++) {
            char c = dsl.q.charAt(dsl.i);
            if (quotesOrEscapes(c, dsl, predicates))
                continue;

            switch (c) {
                case '{' :
                    dsl.openSubQuery(predicates);
                    break;
                case '}' :
                    dsl.shortCircuit = true;
                    dsl.checkExpression(predicates);
                    break;
                case '(':
                    predicates = dsl.open(predicates);
                    break;
                case ')':
                    if (dsl.close(predicates)) {
                        return dsl.str.isEmpty()
                                ? predicates
                                : addExpression(dsl.getString(), dsl, predicates);
                    }
                    break;
                case ' ':
                    dsl.checkExpression(predicates);
                    break;
                default:
                    dsl.str.add(c);
                    checkFinal(dsl, predicates);
                    break;
            }
        }
        if (dsl.isOpen())
            throw new BadQueryException("no matching closing paren ')'", dsl.i);

        return predicates;
    }

    private static <T> Predicate parsePredicates(Predicates predicates
            , Path<T> root
            , AbstractQuery<?> criteriaQuery) {
        if(Objects.nonNull(predicates.where))
            criteriaQuery.where(predicates.where);
        if (Objects.nonNull(predicates.having)) {
            predicates.grouping.add(root);
            criteriaQuery.groupBy(predicates.groupBy()).having(predicates.having);
        }
        return predicates.where == null ? predicates.having : predicates.where;
    }

    private static <T> boolean quotesOrEscapes(char c, DSL<T> dsl, Predicates predicates) {
        if (c == QUOTE) {
            if (dsl.escaped) {
                dsl.str.add(c);
                dsl.unescape();
            } else {
                dsl.toggleQuote();
            }
            checkFinal(dsl, predicates);
            return true;
        } else if (dsl.quoted) {
            if (c == '\\' && !dsl.escaped) {
                dsl.escape();
            } else {
                dsl.str.add(c);
            }
            return true;
        } else if (c == '\\') {
            dsl.escape();
            return true;
        } else if (dsl.collecting) {
            if (c == ']') {
                dsl.closeCollection();
                checkFinal(dsl, predicates);
            } else {
                if (c == ',')
                    dsl.collect();
                else
                    dsl.str.add(c);
            }
            return true;
        } else if (c == '[') {
            dsl.openCollection();
            return true;
        }
        return false;
    }

    private static <T> void checkFinal(DSL<T> dsl
            , Predicates predicates) {
        if (dsl.isLast() && (dsl.str.isNotEmpty() || Str.isNotEmpty(dsl.tmp))) {
            addExpression(dsl.getString(), dsl, predicates);
        }
    }

    protected static <T> Predicates addExpression(String s
            , DSL<T> dsl
            , Predicates predicates) {
        Expression<T> expression = dsl.list == null
                ? new Expression<>(s)
                : new Expression<>(dsl.tmp, dsl.list);
        Predicate p = expression.apply(dsl);
        boolean isOr = Logical.Operator.or.name().equals(dsl.logical.peek());
        if (expression.isAggregate()) {
            if(isOr && !dsl.isHaving)
                throw new BadQueryException("Cannot use OR between aggregate and normal expressions. Use collector operator instead", dsl.i);

            if (expression.f1 == null && Objects.nonNull(expression.p1))
                predicates.grouping.add(expression.p1);
            else if (expression.f2 == null && expression.prop && Objects.nonNull(expression.p2))
                predicates.grouping.add(expression.p2);

            predicates.having = Logical.apply(predicates.having, dsl.logical, dsl.criteriaBuilder, p);
            dsl.isHaving = true;
        } else {
            if(isOr && dsl.isHaving)
                throw new BadQueryException("Cannot use OR between aggregate and normal expressions. Use collector operator instead", dsl.i);

            predicates.where = Logical.apply(predicates.where, dsl.logical, dsl.criteriaBuilder, p);
            dsl.isHaving = false;
        }

        return predicates;
    }

    protected final String q;
    protected final Path<?> root;
    protected final Path<?> parent;
    protected final AbstractQuery<?> query;
    protected final CriteriaBuilder criteriaBuilder;
    protected final Str str = Str.empty();
    protected final Stack<String> logical = new Stack<String>() {{ push(Logical.Operator.and.name()); }};
    protected final Stack<String> groups = new Stack<>();

    protected String tmp;
    protected int i = 0;
    protected int depth = 0;
    protected final int length;
    protected boolean quoted = false;
    protected boolean escaped = false;
    protected boolean collecting = false;
    protected boolean shortCircuit = false;
    protected boolean function = false;
    protected List<Object> list;
    boolean isHaving;

    DSL(String q, Root<T> root, AbstractQuery<?> query, CriteriaBuilder criteriaBuilder) {
        this.q = q;
        this.root = root;
        this.parent = null;
        this.query = query;
        this.criteriaBuilder = criteriaBuilder;
        this.length = q == null ? 0 : q.length();
    }

    DSL(String q, int i, Path<?> path, Path<?> parent, Subquery<T> query, CriteriaBuilder criteriaBuilder) {
        this.q = q;
        this.root = path;
        this.parent = parent;
        this.query = query;
        this.criteriaBuilder = criteriaBuilder;
        this.i = i;
        this.length = q == null ? 0 : q.length();
    }

    protected void escape() { escaped = true; }
    protected void unescape() { escaped = false; }

    protected void toggleQuote() {
        quoted = !quoted;
    }

    protected DSL<T> increment() {
        i++;
        depth++;
        return this;
    }

    protected Predicates open(Predicates predicates) {
        if (isFunctionName()) {
            function = true;
            str.add('(');
        } else {
            groups.push("(");
            if (logical.size() > 0 && Logical.isValid(logical.peek())) {
                Predicates newPredicates = predicates(increment());
                String logic = logical.pop();

                if (Objects.nonNull(newPredicates.where)) {
                    if (predicates.where == null)
                        predicates.where = newPredicates.where;
                    else
                        predicates.where = Logical.apply(logic
                                , criteriaBuilder
                                , predicates.where
                                , newPredicates.where);
                }
                if (Objects.nonNull(newPredicates.having)) {
                    predicates.grouping.addAll(newPredicates.grouping);
                    if (predicates.having == null)
                        predicates.having = newPredicates.having;
                    else
                        predicates.having = Logical.apply(logic
                                , criteriaBuilder
                                , predicates.having
                                , newPredicates.having);
                }
            } else {
                predicates = predicates(increment());
            }
        }
        return predicates;
    }

    /**
     * Execute a DSL close parentheses operation
     * @param predicates The predicates representing the DSL
     * @return True if the parentheses are nested
     * @throws BadQueryException if there is no matching parentheses
     */
    protected boolean close(Predicates predicates) {
        if (function) {
            function = false;
            str.add(')');
            checkFinal(this, predicates);
        } else {
            if (groups.isEmpty())
                throw new BadQueryException("no matching opening paren '('", i);
            groups.pop();
            if (depth == 0) {
                checkFinal(this, predicates);
            } else {
                depth--;
                return true;
            }
        }
        return false;
    }

    protected boolean isOpen() {
        return groups.size() > 0;
    }

    protected void openCollection() {
        this.collecting = true;
        this.list = new ArrayList<>();
        this.tmp = getString();
    }

    protected void closeCollection() {
        this.collect();
        this.collecting = false;
    }

    protected void collect() {
        if (str.trim().isInt()) {
            list.add(str.toInteger().orElse(0));
        } else if (str.isNum()) {
            list.add(str.toDouble().orElse(0.0));
        } else {
            if (str.startsWith("'") && str.endsWith("'"))
                str.trim("'");
            list.add(str.toString());
        }
        str.delete();
    }
    boolean isLast() { return i == length - 1; }
    boolean isFunctionName() {
        String name = str.toString().trim();
        int i = name.lastIndexOf(':');
        return Expression.Function.isValid(i < 0 ? name : name.substring(i + 1));
    }
    boolean isLogicalName() { return Logical.isValid(str.toString().trim()); }

    protected void openSubQuery(Predicates predicates) {
        if (str.endsWith(Expression.COLLECTOR_REF)) {
            if (parent == null || root.getClass().getName().contains("SingularAttribute"))
                collectorSubQuery(predicates);
            else
                collectorSubQueryJoin(predicates);
        } else {
            subQuery(predicates);
        }
    }

    protected void collectorSubQuery(Predicates predicates) {
        Subquery<Integer> subQuery = query.subquery(Integer.class);
        From<?, ?> subRoot = subQuery.from(root.getJavaType());
        CriteriaBuilder cb = criteriaBuilder;
        String[] keys = getString().split(Expression.METHOD_REF)[0].split(",");
        javax.persistence.criteria.Expression<?>[] groups = new javax.persistence.criteria.Expression[keys.length];
        Predicate[] correlatedPredicates = new Predicate[keys.length];
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i].trim();
            Path<?> subPath = Expression.toPath(subRoot, k);
            Path<?> path = Expression.toPath(root, k);
            correlatedPredicates[i] = cb.equal(subPath, path);
            groups[i] = subPath;
        }
        DSL<?> subDSL = new DSL<>(q, i + 1, subRoot, root, subQuery, cb);
        Predicates subPredicates = predicates(subDSL);
        subPredicates.where = subPredicates.where == null
                ? cb.and(correlatedPredicates)
                : cb.and(cb.and(correlatedPredicates), subPredicates.where);
        parsePredicates(subPredicates, subRoot, subQuery);
        subQuery.select(cb.literal(1)).groupBy(groups);
        predicates.where = Logical.apply(predicates.where, logical, cb, cb.exists(subQuery));
        i = subDSL.i;
    }

    protected void collectorSubQueryJoin(Predicates predicates) {
        Subquery<Integer> subQuery = query.subquery(Integer.class);
        From<?, ?> subRoot = parent instanceof Root
                ? subQuery.correlate((Root<?>)parent)
                : subQuery.correlate((Join<?, ?>)parent);

        From<?, ?> join = subRoot.join(((Join<?, ?>) root).getAttribute().getName());
        CriteriaBuilder cb = criteriaBuilder;
        String[] keys = getString().split(Expression.METHOD_REF)[0].split(",");
        List<javax.persistence.criteria.Expression<?>> groups = new ArrayList<>();
        for (String key : keys) {
            String k = key.trim();
            if (k.isEmpty()) continue;
            Path<?> subPath = Expression.toPath(join, k);
            groups.add(subPath);
        }
        DSL<?> subDSL = new DSL<>(q, i + 1, join, root, subQuery, cb);
        parsePredicates(predicates(subDSL), subRoot, subQuery);
        subQuery.select(cb.literal(1));
        subQuery.groupBy(groups);

        predicates.where = Logical.apply(predicates.where, logical, cb, cb.exists(subQuery));
        i = subDSL.i;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void subQuery(Predicates predicates) {
        From<?, ?> subRoot;
        Subquery<?> subQuery = query.subquery(root.getJavaType());
        if (root instanceof Root) {
            subRoot = subQuery.correlate((Root<?>)root);
        } else {
            subRoot = subQuery.correlate((Join<?, ?>)root);
        }
        Join join = subRoot.join(getString());
        DSL<?> subDSL = new DSL<>(q, i + 1, join, root, subQuery, criteriaBuilder);
        subQuery.select((javax.persistence.criteria.Expression)subRoot);
        parsePredicates(predicates(subDSL), subRoot, subQuery);
        predicates.where = Logical.apply(predicates.where, logical, criteriaBuilder, criteriaBuilder.exists(subQuery));
        i = subDSL.i;
    }

    protected void checkExpression(Predicates predicates) {
        if (Objects.nonNull(list) && list.size() > 0) {
            addExpression(getString(), this, predicates);
            list = null;
        } else if(str.contains(":")) {
            addExpression(getString(), this, predicates);
        } else if (isLogicalName()) {
            logical.push(getString());
        }
    }

    protected String getString() {
        String s = str.trim().toString();
        str.delete();
        return s;
    }
}
