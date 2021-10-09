package entity;

import cn.xnatural.jpa.LongIdEntity;

import javax.persistence.Entity;

@Entity
public class Test extends LongIdEntity {
    private Integer age;
    private String name;

    public Integer getAge() {
        return age;
    }

    public Test setAge(Integer age) {
        this.age = age;
        return this;
    }

    public String getName() {
        return name;
    }

    public Test setName(String name) {
        this.name = name;
        return this;
    }
}
