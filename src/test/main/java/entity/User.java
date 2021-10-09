package entity;

import cn.xnatural.jpa.IEntity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user")
public class User implements IEntity {
    @Id
    public String Host;
    private String User;

    public User() { }

    public User(String host, String user) {
        Host = host;
        User = user;
    }

    public String getUser() {
        return User;
    }

    public entity.User setUser(String user) {
        User = user;
        return this;
    }

    @Override
    public String toString() {
        return "User@" + Integer.toHexString(hashCode()) +
                "{Host='" + Host + '\'' +
                ", User='" + User + '\'' +
                '}';
    }
}
