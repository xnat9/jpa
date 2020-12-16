package cn.xnatural.jpa;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * long 为主键的实体
 */
@MappedSuperclass
public class LongIdEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }

    public LongIdEntity setId(Long id) {
        this.id = id;
        return this;
    }
}