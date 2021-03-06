package cn.xnatural.jpa;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;


@MappedSuperclass
public class GUIDEntity extends BaseEntity {
    @Id
    @GeneratedValue(generator = "guid")
    @GenericGenerator(name = "guid", strategy = "org.hibernate.id.GUIDGenerator")
    @Column(length = 50)
    private String id;

    public String getId() { return id; }

    public GUIDEntity setId(String id) {
        this.id = id;
        return this;
    }
}
