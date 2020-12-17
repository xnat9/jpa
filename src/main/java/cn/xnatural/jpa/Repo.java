package cn.xnatural.jpa;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.query.Query;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 对应一个数据源
 */
public class Repo {
    protected static final Logger       log = LoggerFactory.getLogger(Repo.class);
    /**
     * 属性集: 包含 datasource, hibernate 两部分
     */
    protected final Map<String, Object> attrs;
    /**
     * {@link SessionFactory}
     */
    protected       SessionFactory      sf;
    /**
     * 当前数据源
     */
    protected       DataSource          datasource;
    /**
     * 关联哪些实体
     */
    protected final List<Class>         entities = new LinkedList<>();


    public Repo() { this((Map<String, Object>) null); }

    /**
     * 连接url
     * @param url
     */
    public Repo(String url) {
        this.attrs = new ConcurrentHashMap<>();
        attrs.put("url", url); attrs.put("jdbcUrl", url);
    }

    /**
     *
     * @param jdbcUrl jdbc连接串
     * @param username 用户名
     * @param password 密码
     * @param minIdle 最小空闲连接
     * @param maxActive 最大活动连接
     */
    public Repo(String jdbcUrl, String username, String password, Integer minIdle, Integer maxActive) {
        this.attrs = new ConcurrentHashMap<>();
        attrs.put("url", jdbcUrl); attrs.put("jdbcUrl", jdbcUrl);
        attrs.put("minIdle", minIdle); attrs.put("minimumIdle", minIdle);
        attrs.put("maxActive", maxActive); attrs.put("maximumPoolSize", maxActive);
        attrs.put("username", username); attrs.put("password", password);
    }

    public Repo(Map<String, Object> attrs) {
        this.attrs = attrs == null ? new ConcurrentHashMap<>() : attrs;
    }


    /**
     * 初始化
     * @return
     */
    public Repo init() {
        if (sf != null) throw new RuntimeException("Already inited");

        //1. 数据源
        if (datasource != null) throw new RuntimeException("DataSource already exist");
        datasource = createDataSource(attrs);
        if (datasource == null) throw new RuntimeException("Not found DataSource implement class");

        //2. 初始化Hibernate. 可配置的属性名 AvailableSettings
        Map<String, Object> props = new HashMap<>(attrs);
        // props.putIfAbsent("hibernate.hbm2ddl.auto", "none");
        props.putIfAbsent("hibernate.physical_naming_strategy", PhysicalNaming.class);
        props.putIfAbsent("hibernate.implicit_naming_strategy", ImplicitNaming.class);
        sf = createSessionFactory(datasource, props, entities);
        return this;
    }


    /**
     * 关闭Repo
     */
    public void close() {
        try {
            sf.close(); sf = null;
            datasource.getClass().getMethod("close").invoke(datasource);
            datasource = null;
        } catch (Exception e) {}
    }


    /**
     * 添加被管理的实体类
     * @param clzs
     * @return
     */
    public Repo entities(Class... clzs) {
        if (sf != null) throw new RuntimeException("Already inited");
        if (clzs == null) return this;
        for (Class clz : clzs) { entities.add(clz); }
        return this;
    }


    // 事务的线程标记
    protected static final ThreadLocal<Boolean> txFlag = ThreadLocal.withInitial(() -> false);
    /**
     * 事务执行方法
     * @param fn 数据库操作函数
     * @param okFn 执行成功后回调
     * @param failFn 执行失败后回调
     * @return
     */
    public <T> T trans(Function<Session, T> fn, Runnable okFn, Consumer<Exception> failFn) {
        if (sf == null) throw new RuntimeException("Please init first");
        Session s = sf.getCurrentSession();
        // 当前线程存在事务
        if (txFlag.get()) return fn.apply(s);
        else { // 当前线程没有事务,开启新事务
            Transaction tx = s.getTransaction();
            tx.begin(); txFlag.set(true);
            Exception ex = null;
            try {
                T r = fn.apply(s); tx.commit(); txFlag.set(false); s.close();
                return r;
            } catch (Exception t) {
                tx.rollback(); txFlag.set(false); ex = t; s.close();
                if (failFn == null) throw t;
            } finally {
                if (ex != null) {
                    if (failFn != null) failFn.accept(ex);
                } else {// 成功
                    if (okFn != null) okFn.run();
                }
            }
        }
        return null;
    }


    /**
     * {@link #trans(Function, Runnable, Consumer)}
     * @param fn 数据库操作函数. 事务
     * @param <T>
     * @return
     */
    public <T> T trans(Function<Session, T> fn) { return trans(fn, null, null); }


    /**
     * 根据实体类, 查表名字
     * @param eType
     * @return 表名
     */
    public String tbName(Class<IEntity> eType) {
        if (sf == null) throw new RuntimeException("Please init first");
        return ((AbstractEntityPersister) ((MetamodelImplementor) sf.getMetamodel()).locateEntityPersister(eType)).getRootTableName();
    }


    /**
     * 连接mysql当前数据库的库名
     * @return 数据库名
     */
    public String getDbName() {
         return ((SessionFactoryImpl) sf).getJdbcServices().getJdbcEnvironment().getCurrentCatalog().getText();
    }


    /**
     * 连接 jdbcUrl
     * @return 连接 jdbcUrl
     */
    public String getJdbcUrl() {
        if (sf == null) throw new RuntimeException("Please init first");
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(datasource.getClass()).getPropertyDescriptors()) {
                if (pd.getName().equals("jdbcUrl")) return (String) pd.getReadMethod().invoke(datasource);
                if (pd.getName().equals("url")) return (String) pd.getReadMethod().invoke(datasource);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }


    /**
     * 保存/更新实体
     * @param entity
     * @return <E>
     */
    public <E extends IEntity> E saveOrUpdate(E entity) {
        return trans((se) -> {
            if (entity instanceof BaseEntity) {
                Date d = new Date();
                if (((BaseEntity) entity).getCreateTime() == null) ((BaseEntity) entity).setCreateTime(d);
                ((BaseEntity) entity).setUpdateTime(d);
            }
            se.saveOrUpdate(entity);
            return entity;
        });
    }


    /**
     * 根据id查找实体
     * @param eType 实体类型
     * @param id id
     * @return
     */
    public <T extends IEntity> T findById(Class<T> eType, Serializable id) {
        if (eType == null) throw new IllegalArgumentException("eType must not be null");
        return trans((se) -> se.get(eType, id));
    }


    /**
     * 查询
     * @param eType 实体类型
     * @param spec 条件
     * @return <T> 实体对象
     */
    public <T extends IEntity> T find(Class<T> eType, CriteriaSpec spec) {
        return trans((s) -> {
                CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<T> query = cb.createQuery(eType);
            Root<T> root = query.from(eType);
            Object p = spec == null ? null : spec.toPredicate(root, query, cb);
            if (p instanceof Predicate) query.where((Predicate) p);
            List<T> ls = s.createQuery(query).setMaxResults(1).list();
            return (ls.size() == 1 ? ls.get(0) : null);
        });
    }


    /**
     * 删实体
     * @param entity 实体对象
     * @param <E>
     */
    public <E extends IEntity> void delete(E entity) { trans(s -> {s.delete(entity); return null;});}


    /**
     * 根据id删除
     * NOTE: 被删除的实体主键名必须为 "id"
     * @param eType 实体类型
     * @param id id值
     * @return true: 删除成功
     */
    public <E extends IEntity> boolean delete(Class<E> eType, Serializable id) {
        if (eType == null) throw new IllegalArgumentException("eType must not be null");
        return trans(s -> s.createQuery("delete from " + eType.getSimpleName() + " where id=:id")
                .setParameter("id", id)
                .executeUpdate() > 0
        );
    }


    /**
     * sql 查询 一行数据
     * @param sql sql 语句
     * @param params 参数
     * @return
     */
    public Map<String, Object> firstRow(String sql, Object...params) {
        return (Map<String, Object>) trans(se -> {
            NativeQueryImplementor query = se.createNativeQuery(sql).unwrap(NativeQueryImpl.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    query.setParameter(i+1, params[i]);
                }
            }
            List ls = query.setMaxResults(1).list();
            return ls == null || ls.isEmpty() ? null : ls.get(0);
        });
    }


    /**
     * sql 查询
     * @param sql sql
     * @param params 参数
     * @return
     */
    public List<Map<String, Object>> rows(String sql, Object...params) {
        return trans(se -> {
            NativeQueryImplementor query = se.createNativeQuery(sql).unwrap(NativeQueryImpl.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    query.setParameter(i+1, params[i]);
                }
            }
            return query.list();
        });
    }


    /**
     * 查询多条数据
     * @param eType 实体类型
     * @param spec 条件
     * @return
     */
    public <E extends IEntity> List<E> findList(Class<E> eType, CriteriaSpec spec) {
        return findList(eType, null, null, spec);
    }


    /**
     * 查询多条数据
     * @param eType 实体类型
     * @param start 开始行 从0开始
     * @param limit 条数限制
     * @param spec 条件
     * @return list
     */
    public <E extends IEntity> List<E> findList(Class<E> eType, Integer start, Integer limit, CriteriaSpec spec) {
        if (eType == null) throw new IllegalArgumentException("eType must not be null");
        if (start != null && start < 0) throw new IllegalArgumentException("start must >= 0 or not give");
        if (limit != null && limit <= 0) throw new IllegalArgumentException("limit must > 0 or not give");
        return trans(s -> {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<E> cQuery = cb.createQuery(eType);
            Root<E> root = cQuery.from(eType);
            Object p = spec == null ? null : spec.toPredicate(root, cQuery, cb);
            if (p instanceof Predicate) cQuery.where((Predicate) p);
            Query<E> query = s.createQuery(cQuery);
            if (start != null) query.setFirstResult(start);
            if (limit != null) query.setMaxResults(limit);
            return query.list();
        });
    }


    /**
     * 分页查询
     * @param eType 实体类型
     * @param page 当前第几页. >=1
     * @param pageSize 每页大小 >=1
     * @param <E>
     * @return {@link Page<E>}
     */
    public <E extends IEntity> Page<E> findPage(Class<E> eType, Integer page, Integer pageSize) {
        return findPage(eType, page, pageSize, null);
    }


    /**
     * 分页查询
     * @param eType
     * @param page 当前第几页. >=1
     * @param pageSize 每页大小 >=1
     * @param spec 条件
     * @return {@link Page<E>}
     */
    public <E extends IEntity> Page<E> findPage(Class<E> eType, Integer page, Integer pageSize, CriteriaSpec spec) {
        if (eType == null) throw new IllegalArgumentException("eType must not be null");
        if (page == null || page < 1) throw new IllegalArgumentException("page: " + page + ", must >=1");
        if (pageSize == null || pageSize < 1) throw new IllegalArgumentException("pageSize: " + pageSize + ", must >=1");
        return trans(s -> {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<E> query = cb.createQuery(eType);
            Root<E> root = query.from(eType);
            Object p = spec == null ? null : spec.toPredicate(root, query, cb);
            if (p instanceof Predicate) query.where((Predicate) p);
            return new Page<E>().setPage(page).setPageSize(pageSize).setList(
                    s.createQuery(query).setFirstResult((page - 1) * pageSize).setMaxResults(pageSize).list()
            ).setTotalRow(count(eType, spec));
        });
    }


    /**
     * 统计某张表总数
     * @param eType 实体类型
     * @param <E>
     * @return
     */
    public <E extends IEntity> long count(Class<E> eType) { return count(eType, null); }


    /**
     * 根据实体类, 统计
     * @param eType 实体类型
     * @param spec 条件
     * @return
     */
    public <E extends IEntity> long count(Class<E> eType, CriteriaSpec spec) {
        if (eType == null) throw new IllegalArgumentException("eType must not be null");
        return trans(s -> {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<E> root = query.from(eType);
            Object p = spec == null ? null : spec.toPredicate(root, query, cb);
            if (query.isDistinct()) query.select(cb.countDistinct(root));
            else query.select(cb.count(root));
            query.orderBy(Collections.emptyList());
            if (p instanceof Predicate) query.where((Predicate) p);
            return s.createQuery(query).getSingleResult();
        });
    }


    /**
     * getter
     * @return {@link SessionFactory}
     */
    public SessionFactory getSessionFactory() { return sf; }


    /**
     * 创建 SessionFactory
     * @param datasource 数据源
     * @param props hibernate 属性
     * @param entities 实体类
     * @return {@link SessionFactory}
     */
    public static SessionFactory createSessionFactory(DataSource datasource, Map props, List<Class> entities) {
        props = props == null ? new HashMap<>() : props;
        props.putIfAbsent("hibernate.current_session_context_class", "thread"); // 会话和 线程绑定
        props.putIfAbsent("hibernate.temp.use_jdbc_metadata_defaults", "true"); // 自动探测连接的数据库信息,该用哪个Dialect
        MetadataSources ms = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .addService(ConnectionProvider.class, new ConnectionProvider() {
                            @Override
                            public Connection getConnection() throws SQLException { return datasource.getConnection(); }

                            @Override
                            public void closeConnection(Connection conn) throws SQLException { conn.close(); }

                            @Override
                            public boolean supportsAggressiveRelease() { return true; }

                            @Override
                            public boolean isUnwrappableAs(Class unwrapType) {
                                return ConnectionProvider.class.equals( unwrapType ) || DataSource.class.isAssignableFrom( unwrapType );
                            }

                            @Override
                            public <T> T unwrap(Class<T> unwrapType) {
                                if (ConnectionProvider.class.equals(unwrapType)) { return (T) this; }
                                else if ( DataSource.class.isAssignableFrom(unwrapType ) ) { return (T) datasource; }
                                else { throw new UnknownUnwrapTypeException( unwrapType ); }
                            }
                        })
                        .applySettings(props).build()
        );
        for (Class clz : entities) { ms.addAnnotatedClass(clz); }
        return ms.buildMetadata().buildSessionFactory();
    }


    /**
     * 创建一个 数据源
     * @param dsAttr 连接池属性
     * @return {@link DataSource} 数据源
     */
    public static DataSource createDataSource(Map<String, Object> dsAttr) {
        DataSource ds = null;
        // druid 数据源
        try {
            Map props = new HashMap();
            dsAttr.forEach((s, o) -> props.put(s, Objects.toString(o, "")));
            // if (!props.containsKey("validationQuery")) props.put("validationQuery", "select 1") // oracle
            if (!props.containsKey("filters")) { // 默认监控慢sql
                props.put("filters", "stat");
            }
            if (!props.containsKey("connectionProperties")) {
                // com.alibaba.druid.filter.stat.StatFilter
                props.put("connectionProperties", "druid.stat.logSlowSql=true;druid.stat.slowSqlMillis=5000");
            }
            ds = (DataSource) Class.forName("com.alibaba.druid.pool.DruidDataSourceFactory").getMethod("createDataSource", Map.class).invoke(null, props);
        }
        catch(ClassNotFoundException ex) {}
        catch(Exception ex) { throw new RuntimeException(ex); }

        if (ds == null) {// Hikari 数据源
            try {
                Class<?> clz = Class.forName("com.zaxxer.hikari.HikariDataSource");
                ds = (DataSource) clz.newInstance();
                for (PropertyDescriptor pd : Introspector.getBeanInfo(clz).getPropertyDescriptors()) {
                    Object v = dsAttr.get(pd.getName());
                    if (v != null) {
                        if (Integer.class.equals(pd.getPropertyType())  || int.class.equals(pd.getPropertyType())) pd.getWriteMethod().invoke(ds, Integer.valueOf(v.toString()));
                        else if (Long.class.equals(pd.getPropertyType()) || long.class.equals(pd.getPropertyType())) pd.getWriteMethod().invoke(ds, Long.valueOf(v.toString()));
                        else if (Boolean.class.equals(pd.getPropertyType()) || boolean.class.equals(pd.getPropertyType())) pd.getWriteMethod().invoke(ds, Boolean.valueOf(v.toString()));
                        else pd.getWriteMethod().invoke(ds, v);
                    }
                }
            }
            catch(ClassNotFoundException ex) {}
            catch(Exception ex) { throw new RuntimeException(ex); }
        }

        // dbcp2 数据源
        if (ds == null) {
            try {
                Properties props = new Properties();
                dsAttr.forEach((s, o) -> props.put(s, Objects.toString(o, "")));
                // if (!props.containsKey("validationQuery")) props.put("validationQuery", "select 1");
                ds = (DataSource) Class.forName("org.apache.commons.dbcp2.BasicDataSourceFactory").getMethod("createDataSource", Properties.class).invoke(null, props);
            }
            catch(ClassNotFoundException ex) {}
            catch(Exception ex) { throw new RuntimeException(ex); }
        }
        return ds;
    }
}
