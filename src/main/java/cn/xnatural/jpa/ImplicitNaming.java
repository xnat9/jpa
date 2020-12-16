package cn.xnatural.jpa;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

public class ImplicitNaming extends ImplicitNamingStrategyJpaCompliantImpl {
    @Override
    public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
        String name = source.getOwningPhysicalTableName() + "_"+ source.getAssociationOwningAttributePath().getProperty();
        return toIdentifier(name, source.getBuildingContext());
    }
}