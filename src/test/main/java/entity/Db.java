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

    @Override
    public String toString() {
        return "Db{" +
                "Db='" + Db + '\'' +
                ", Host='" + Host + '\'' +
                '}';
    }
}
