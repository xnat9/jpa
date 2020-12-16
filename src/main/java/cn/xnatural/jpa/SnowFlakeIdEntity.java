package cn.xnatural.jpa;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;


/**
 * SnowFlake id 生成策略
 */
@MappedSuperclass
public class SnowFlakeIdEntity extends BaseEntity {
    @Id
    @GeneratedValue(generator = "snowFlakeId")
    @GenericGenerator(name = "snowFlakeId", strategy = "core.module.jpa.SnowFlakeIdGenerator")
    private Long id;

    public Long getId() {
        return id;
    }

    public SnowFlakeIdEntity setId(Long id) {
        this.id = id;
        return this;
    }
}
