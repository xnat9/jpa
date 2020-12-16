package cn.xnatural.jpa;

import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.MappedSuperclass;
import java.util.Date;

/**
 * 基本实体用来被继承
 */
@MappedSuperclass
@DynamicUpdate
public class BaseEntity implements IEntity {
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;


    public Date getCreateTime() {
        return createTime;
    }

    public BaseEntity setCreateTime(Date createTime) {
        this.createTime = createTime;
        return this;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public BaseEntity setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }
}
