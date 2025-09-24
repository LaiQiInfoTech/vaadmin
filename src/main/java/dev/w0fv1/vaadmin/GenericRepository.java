package dev.w0fv1.vaadmin;

import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Supplier;

/**
 * 通用仓库：封装了 <b>CRUD / 分页 / 动态过滤 / 多字段排序</b> 功能。<br/>
 * - 通过 {@link PredicateManager} 组合 <code>CriteriaBuilder</code> 过滤条件；<br/>
 * - 通过 {@link SortOrder} 列表完成服务器端多字段 ASC/DESC 排序。<br/>
 */
@Slf4j
@Component
@Repository
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class GenericRepository {

    /* -------------------------------------------------- DI -------------------------------------------------- */
    @PersistenceContext
    private EntityManager entityManager;
    private final TransactionTemplate txTemplate;

    /* -------------------------------------------------- Tx helpers -------------------------------------------------- */
    public <T> T execute(TransactionCallback<T> cb) throws TransactionException {
        return txTemplate.execute(cb);
    }

    public <T> T execute(Supplier<T> cb) {
        return txTemplate.execute(st -> safeGet(cb, st));
    }

    public void execute(Runnable run) {
        txTemplate.execute(st -> {
            safeRun(run, st);
            return null;
        });
    }
    @Transactional
    public <T> Boolean exist(Long id, Class<T> clazz) {
        return entityManager.find(clazz, id) != null;
    }

    private <T> T safeGet(Supplier<T> s, TransactionStatus st) {
        try {
            return s.get();
        } catch (Exception e) {
            st.setRollbackOnly();
            log.error("Tx failure", e);
            return null;
        }
    }

    private void safeRun(Runnable r, TransactionStatus st) {
        try {
            r.run();
        } catch (Exception e) {
            st.setRollbackOnly();
            log.error("Tx failure", e);
        }
    }

    /* -------------------------------------------------- Basic CRUD -------------------------------------------------- */
    @Transactional
    public <T extends BaseManageEntity<?>> T save(T e) {
        if (e.getId() == null) entityManager.persist(e);
        else e = entityManager.merge(e);
        entityManager.flush();
        return e;
    }

    @Transactional
    public <T> void delete(T e) {
        entityManager.remove(e);
        entityManager.flush();
    }

    /* -------------------------------------------------- Page + Filter + Sort -------------------------------------------------- */

    /**
     * 排序描述
     */
    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class SortOrder {
        public SortOrder(QuerySortOrder querySortOrder) {
            this.property = querySortOrder.getSorted();
            if (querySortOrder.getDirection().equals(SortDirection.ASCENDING)) {
                this.direction = Direction.ASC;
            } else if (querySortOrder.getDirection().equals(SortDirection.DESCENDING)) {
                this.direction = Direction.DESC;
            } else {
                this.direction = Direction.ASC;
            }
        }

        public enum Direction {ASC, DESC}

        private final String property;
        private final Direction direction;


    }

    /**
     * 带条件 & 排序的分页查询
     *
     * @param type       实体类型
     * @param page       第几页（从 0）
     * @param size       每页大小
     * @param pm         条件管理器
     * @param sortOrders 排序
     */
    @Transactional
    public <T> List<T> getPage(Class<T> type, int page, int size, PredicateManager<T> pm, List<SortOrder> sortOrders) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(type);
        Root<T> root = cq.from(type);

        // where 条件
        List<jakarta.persistence.criteria.Predicate> preds = pm.buildPredicates(cb, root);
        if (!preds.isEmpty()) {
            cq.where(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }

        // order by
        if (sortOrders != null && !sortOrders.isEmpty()) {
            List<Order> orders = sortOrders.stream()
                    .map(so -> so.direction == SortOrder.Direction.ASC
                            ? cb.asc(root.get(so.property))
                            : cb.desc(root.get(so.property)))
                    .toList();
            cq.orderBy(orders);
        } else {
            // 默认按 id 升序
            cq.orderBy(cb.asc(root.get("id")));
        }

        TypedQuery<T> q = entityManager.createQuery(cq);
        q.setFirstResult(page * size);
        q.setMaxResults(size);
        return q.getResultList();
    }



    /* -------------------------------------------------- Total size -------------------------------------------------- */
    @Transactional
    public <T> Long getTotalSize(Class<T> type, PredicateManager<T> pm) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<T> root = cq.from(type);
        cq.select(cb.count(root));
        List<jakarta.persistence.criteria.Predicate> preds = pm.buildPredicates(cb, root);
        if (!preds.isEmpty()) cq.where(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        return entityManager.createQuery(cq).getSingleResult();
    }

    /* -------------------------------------------------- Predicate utilities -------------------------------------------------- */
    @FunctionalInterface
    public interface PredicateBuilder<T> {
        void build(CriteriaBuilder cb, Root<T> root, List<jakarta.persistence.criteria.Predicate> preds);
    }


    public static class PredicateManager<T> {
        private final Map<String, PredicateBuilder<T>> predicateBuilders = new HashMap<>();

        // Add or replace a predicate builder with a key
        public void putPredicate(String key, PredicateBuilder<T> builder) {
            predicateBuilders.put(key, builder);
        }

        // Remove a predicate builder by key
        public void removePredicate(String key) {
            predicateBuilders.remove(key);
        }
        // Add or replace multiple predicate builders at once
        public void addAllPredicates(Map<String, PredicateBuilder<T>> builders) {
            predicateBuilders.putAll(builders);
        }
        // Clear all predicates
        public void clearPredicates() {
            predicateBuilders.clear();
        }

        public void clearPredicatesWithOut(String... keysToKeep) {
            // Convert the keysToKeep array to a set for faster lookup
            Set<String> keysSet = new HashSet<>(Arrays.asList(keysToKeep));

            // Remove all predicates whose key is not in the specified keys
            predicateBuilders.entrySet().removeIf(entry -> !keysSet.contains(entry.getKey()));
        }

        // Build predicates using the CriteriaBuilder and Root
        public List<Predicate> buildPredicates(CriteriaBuilder cb, Root<T> root) {
            List<Predicate> predicates = new ArrayList<>();
            for (PredicateBuilder<T> builder : predicateBuilders.values()) {
                builder.build(cb, root, predicates);
            }
            return predicates;
        }
    }

    /* -------------------------------------------------- misc 单条查询 / util 方法（选留） -------------------------------------------------- */
    @Transactional
    public <T, ID> T find(ID id, Class<T> type) {
        return entityManager.find(type, id);
    }

    @Transactional
    public <T, ID> List<T> findAll(List<ID> ids, Class<T> type) {
        if (ids == null || ids.isEmpty()) {
            return List.of(); // 返回空列表
        }

        // 构建 JPQL 查询
        String jpql = "SELECT e FROM " + type.getSimpleName() + " e WHERE e.id IN :ids";
        TypedQuery<T> query = entityManager.createQuery(jpql, type);
        query.setParameter("ids", ids);
        List<T> resultList = query.getResultList();
        if (resultList == null) {
            return List.of();
        }
        return resultList;
    }

    @Transactional
    public <T, ID> ID saveAndReturnId(T e) {
        entityManager.persist(e);
        entityManager.flush();
        return (ID) ((BaseManageEntity<?>) e).getId();
    }
}
