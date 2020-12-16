package cn.xnatural.jpa;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Criteria 查询 spec
 * @param <E>
 */
public interface CriteriaSpec<E> {
    E toPredicate(Root<E> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
