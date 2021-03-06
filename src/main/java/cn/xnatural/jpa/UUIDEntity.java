package cn.xnatural.jpa;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;


/**
 * UUID 为主键的实体
 */
@MappedSuperclass
public class UUIDEntity extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "cn.xnatural.jpa.UUIDGenerator")
    @Column(length = 36)
    private String id;


    public String getId() { return id; }


    public UUIDEntity setId(String id) {
        this.id = id;
        return this;
    }
}
