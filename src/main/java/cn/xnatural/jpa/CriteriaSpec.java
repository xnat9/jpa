package cn.xnatural.jpa;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Criteria 查询 spec
 * @param <E>
 */
public interface CriteriaSpec<E> {
    /**
     * 查询条件构建
     * @param root
     * @param query
     * @param cb
     * @return
     */
    E toPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
