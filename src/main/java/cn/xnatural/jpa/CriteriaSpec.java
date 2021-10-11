package cn.xnatural.jpa;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Criteria 查询 spec
 * @param <E>
 */
@FunctionalInterface
public interface CriteriaSpec<E> {
    /**
     * 查询条件构建
     * @param root Root
     * @param query CriteriaQuery
     * @param cb CriteriaBuilder
     * @return E
     */
    Object toPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
