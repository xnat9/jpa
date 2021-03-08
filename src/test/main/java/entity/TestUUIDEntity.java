package entity;

import cn.xnatural.jpa.UUIDEntity;

import javax.persistence.Entity;

@Entity
public class TestUUIDEntity extends UUIDEntity {
    private String name;

    public String getName() {
        return name;
    }

    public TestUUIDEntity setName(String name) {
        this.name = name;
        return this;
    }
}
