package entity;

import cn.xnatural.jpa.IEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "Db")
public class Db implements IEntity {
    @Id
    public String Db;
    public String Host;
    private String User;

    public String getUser() {
        return User;
    }

    public Db setUser(String user) {
        User = user;
        return this;
    }

    @Override
    public String toString() {
        return "Db@" + Integer.toHexString(hashCode()) +
                "{Db='" + Db + '\'' +
                ", Host='" + Host + '\'' +
                ", User='" + User + '\'' +
                '}';
    }
}
