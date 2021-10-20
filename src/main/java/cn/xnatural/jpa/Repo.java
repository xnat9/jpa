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
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.transform.ResultTransformer;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 对应一个数据源
 */
public class Repo implements AutoCloseable {
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
    protected final List<Class<? extends IEntity>>         entities = new LinkedList<>();


    public Repo() { this((Map<String, Object>) null); }

    /**
     * 连接url
     * @param jdbcUrl jdbc url
     */
    public Repo(String jdbcUrl) {
        this.attrs = new ConcurrentHashMap<>();
        attrs.put("url", jdbcUrl); attrs.put("jdbcUrl", jdbcUrl);
    }

    /**
     * 指定jdbcUrl创建Repo
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

    /**
     * {@link #Repo(String, String, String, Integer, Integer)}
     * @param jdbcUrl jdbc连接串
     * @param username 用户名
     * @param password 密码
     */
    public Repo(String jdbcUrl, String username, String password) { this(jdbcUrl, username, password, 1, 8); }


    /**
     * 根据属性集创建Repo
     * @param attrs 属性集
     */
    public Repo(Map<String, Object> attrs) { this.attrs = attrs == null ? new ConcurrentHashMap<>() : attrs; }


    /**
     * 初始化
     * @return 当前 {@link Repo}
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
    @Override
    public void close() {
        try {
            sf.close(); sf = null;
            datasource.getClass().getMethod("close").invoke(datasource);
            datasource = null;
        } catch (Exception e) {}
    }


    /**
     * 设置 属性
     * @param key 属性key
     * @param value 属性值
     * @return {@link Repo}
     */
    public Repo setAttr(String key, Object value) { attrs.put(key, value); return this; }


    /**
     * 获取属性
     * @param key 属性key
     * @return 属性值
     */
    public Object getAttr(String key) { return attrs.get(key); }


    /**
     * 添加被管理的实体类
     * @param clzs 实体类
     * @return 当前 {@link Repo}
     */
    public Repo entities(Class<? extends IEntity>... clzs) {
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
     * @param <T> 类型
     */
    public <T> T trans(Function<Session, T> fn) { return trans(fn, null, null); }


    /**
     * 根据实体类, 查表名字
     * @param eType 实体Class
     * @return 表名
     */
    public String tbName(Class<? extends IEntity> eType) {
        if (sf == null) throw new RuntimeException("Please init first");
        return ((AbstractEntityPersister) ((MetamodelImplementor) sf.getMetamodel()).locateEntityPersister(eType)).getRootTableName().replace("`", "");
    }


    /**
     * 连接mysql当前数据库的库名
     * @return 数据库名
     */
    public String getDbName() {
        if (sf == null) throw new RuntimeException("Please init first");
         return ((SessionFactoryImpl) sf).getJdbcServices().getJdbcEnvironment().getCurrentCatalog().getText();
    }


    /**
     * 连接的数据库Dialect
     * @return Dialect
     */
    public String getDialect() {
        if (sf == null) throw new RuntimeException("Please init first");
        return ((SessionFactoryImpl) sf).getJdbcServices().getDialect().getClass().getSimpleName();
    }


    /**
     * 连接的数据库版本
     * @return 版本
     */
    public String getDBVersion() {
        if (sf == null) throw new RuntimeException("Please init first");
        String dialect = getDialect();
        if (dialect == null) return null;
        dialect = dialect.toLowerCase();
        if (dialect.contains("mysql") || dialect.contains("maria")) {
            return firstRow("select version()").entrySet().iterator().next().getValue().toString();
        } else if (dialect.contains("h2")) {
            return firstRow("select H2VERSION()").entrySet().iterator().next().getValue().toString();
        }
        return null;
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
     * @param entity 实体
     * @return 实体{@link E}
     */
    public <E extends IEntity> E saveOrUpdate(E entity) {
        if (entity == null) throw new IllegalArgumentException("Param entity required");
        return trans(session -> {
            if (entity instanceof BaseEntity) {
                Date d = new Date();
                if (((BaseEntity) entity).getCreateTime() == null) ((BaseEntity) entity).setCreateTime(d);
                ((BaseEntity) entity).setUpdateTime(d);
            }
            session.saveOrUpdate(entity);
            return entity;
        });
    }


    /**
     * 根据id查找实体
     * @param eType 实体类型
     * @param id id
     * @return 实体 {@link E}
     */
    public <E extends IEntity> E findById(Class<E> eType, Serializable id) {
        if (eType == null) throw new IllegalArgumentException("Param eType required");
        return trans(session -> session.get(eType, id));
    }


    /**
     * 查询
     * @param eType 实体类型
     * @param spec 条件
     * @return 实体{@link E}
     */
    public <E extends IEntity> E find(Class<E> eType, CriteriaSpec<E> spec) {
        if (eType == null) throw new IllegalArgumentException("Param eType required");
        return trans(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<E> query = cb.createQuery(eType);
            Root<E> root = query.from(eType);
            Object p = spec == null ? null : spec.toPredicate(root, query, cb);
            if (p instanceof Predicate) query.where((Predicate) p);
            List<E> ls = session.createQuery(query).setMaxResults(1).list();
            return (ls.size() == 1 ? ls.get(0) : null);
        });
    }


    /**
     * 删实体
     * @param entity 实体对象
     * @param <E> {@link IEntity}
     */
    public <E extends IEntity> void delete(E entity) {
        if (entity == null) throw new IllegalArgumentException("Param entity required");
        trans(session -> {session.delete(entity); return null;});
    }


    /**
     * 根据id删除
     * NOTE: 被删除的实体主键名必须为 "id"
     * @param eType 实体类型
     * @param id id值
     * @return true: 删除成功
     */
    public <E extends IEntity> boolean delete(Class<E> eType, Serializable id) {
        if (eType == null) throw new IllegalArgumentException("Param eType required");
        if (id == null) throw new IllegalArgumentException("Param id required");
        return trans(session -> session.createQuery("delete from " + eType.getSimpleName() + " where id=:id")
                .setParameter("id", id)
                .executeUpdate() > 0
        );
    }


    /**
     * sql update delete insert 执行
     * @param sql sql语句
     * @param params 参数
     * @return 影响条数
     */
    public int execute(String sql, Object...params) {
        if (sql == null || sql.isEmpty()) throw new IllegalArgumentException("Param sql not empty");
        return trans(session -> fillParam(session.createNativeQuery(sql).unwrap(NativeQueryImpl.class), params).executeUpdate());
    }


    /**
     * hql 查询单条记录
     * @param hql hql
     * @param wrap 返回类型class
     * @param params 参数列表
     * @param <R> 类型
     */
    public <R> R hqlFirstRow(String hql, Class<R> wrap,  Object...params) {
        return (R) trans(session -> {
            List ls = fillParam(session.createQuery(hql, wrap), params).list();
            return ls == null || ls.isEmpty() ? null : ls.get(0);
        });
    }


    /**
     * hql 查询多条记录
     * @param hql hql
     * @param wrap 返回的类型
     * @param params 参数
     * @param <R> 类型
     * @return 列表
     */
    public <R> List<R> hqlRows(String hql, Class<R> wrap,  Object...params) {
        return trans(session -> fillParam(session.createQuery(hql, wrap), params).list());
    }


    /**
     * sql 查询 一行数据
     * @param sql sql 语句
     * @param params 参数
     * @return 一条记录 {@link Map}
     */
    public Map<String, Object> firstRow(String sql, Object...params) { return firstRow(sql, Map.class, params); }


    /**
     * sql 查询 一行数据
     * @param sql sql 语句
     * @param wrap 返回结果包装的类型
     * @param params 参数
     * @param <R> 包装类型
     * @return 一条记录 {@link R}
     */
    public <R> R firstRow(String sql, Class<R> wrap, Object...params) {
        if (sql == null || sql.isEmpty()) throw new IllegalArgumentException("Param sql not empty");
        if (wrap == null) throw new IllegalArgumentException("Param warp required");
        return (R) trans(session -> {
            List ls = fillParam(session.createNativeQuery(sql).unwrap(NativeQueryImpl.class).setResultTransformer(warpTransformer(wrap)), params).setMaxResults(1).list();
            return ls == null || ls.isEmpty() ? null : ls.get(0);
        });
    }


    /**
     * sql 多条查询
     * @param sql sql 语句
     * @param params 参数
     * @return 多条记录 {@link List<Map>}
     */
    public List<Map> rows(String sql, Object...params) { return rows(sql, Map.class, params); }


    /**
     * sql 多条查询
     * @param sql sql
     * @param wrap 返回结果包装的类型
     * @param params sql参数
     * @param <R> 包装类型
     * @return 多条记录 {@link List<R> }
     */
    public <R> List<R> rows(String sql, Class<R> wrap, Object...params) {
        if (sql == null || sql.isEmpty()) throw new IllegalArgumentException("Param sql not empty");
        if (wrap == null) throw new IllegalArgumentException("Param warp required");
        return trans(session -> fillParam(session.createNativeQuery(sql).unwrap(NativeQueryImpl.class).setResultTransformer(warpTransformer(wrap)), params).list());
    }


    /**
     * sql 分页查询
     * @param sql sql 语句
     * @param page 第几页 >=1
     * @param pageSize 每页大小 >=1
     * @param params sql参数
     * @return 一页记录 {@link Page<Map>}
     */
    public Page<Map> sqlPage(String sql, Integer page, Integer pageSize, Object...params) {
        return sqlPage(sql, page, pageSize, Map.class, params);
    }


    /**
     * sql 分页查询
     * @param sql sql 语句
     * @param page 第几页 >=1
     * @param pageSize 每页大小 >=1
     * @param wrap 结果包装类型
     * @param params sql参数
     * @param <T> 包装类型
     * @return 一页记录 {@link Page<T> }
     */
    public <T> Page<T> sqlPage(String sql, Integer page, Integer pageSize, Class<T> wrap, Object...params) {
        if (sql == null || sql.isEmpty()) throw new IllegalArgumentException("Param sql not empty");
        if (wrap == null) throw new IllegalArgumentException("Param warp required");
        if (page == null || page < 1) throw new IllegalArgumentException("Param page >=1");
        if (pageSize == null || pageSize < 1) throw new IllegalArgumentException("Param pageSize >=1");
        return trans(session -> {
            // 当前页数据查询
            Query listQuery = fillParam(session.createNativeQuery(sql).unwrap(NativeQueryImpl.class).setResultTransformer(warpTransformer(wrap)), params);
            // 总条数查询
            Query countQuery = fillParam(session.createNativeQuery("select count(1) from (" + sql + ") t1").unwrap(NativeQueryImpl.class), params);
            return new Page<>().setPage(page).setPageSize(pageSize)
                    .setList(listQuery.setFirstResult((page - 1) * pageSize).setMaxResults(pageSize).list())
                    .setTotalRow(((Number) countQuery.setMaxResults(1).getSingleResult()).longValue());
        });
    }


    /**
     * sql 参数装配
     * 1. 位置参数 例 ?
     * 2. 命名参数 例 :name
     * @param query QueryImplementor
     * @param params sql参数
     */
    protected Query fillParam(Query query, Object[] params) {
        if (params == null || params.length < 1) return query;
        List<QueryParameter> nps = new ArrayList<>(query.getParameterMetadata().getNamedParameters());
        if (nps.size() > 0) { //命名参数sql/hql
            Collections.sort(nps, Comparator.comparingInt(o -> o.getSourceLocations()[0]));
            for (int i = 0, length = Math.min(params.length, nps.size()); i < length; i++) {
                Object v = params[i];
                if (v == null) continue;
                if (v instanceof Collection) query.setParameterList(nps.get(i).getName(), (Collection) v);
                else if (v.getClass().isArray()) query.setParameterList(nps.get(i).getName(), (Object[]) v);
                else query.setParameter(nps.get(i).getName(), v);
            }
        } else { //位置参数sql/hql
            for (int i = 0; i < params.length; i++) {
                Object v = params[i];
                if (v == null) continue;
                if (v instanceof Collection) query.setParameterList(i+1, (Collection) v);
                else if (v.getClass().isArray()) query.setParameterList(i+1, (Object[]) v);
                else query.setParameter(i+1, v);
            }
        }
        return query;
    }


    /**
     * 结果解析通用工具
     * @param wrap 类型
     * @return {@link ResultTransformer}
     */
    protected <T> ResultTransformer warpTransformer(Class<T> wrap) {
        return Map.class.isAssignableFrom(wrap) ? Transformers.ALIAS_TO_ENTITY_MAP : new BasicTransformerAdapter() {
            T t;
            @Override
            public Object transformTuple(Object[] tuple, String[] aliases) {
                try {
                    t = wrap.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                for (int i=0; i<tuple.length; i++ ) {
                    String alias = aliases[i];
                    if (alias != null) {
                        try {
                            set(alias, tuple[i]);
                        } catch (Exception e) {
                            log.error("Set property error. type: " + wrap.getName() + ", property: " + alias, e);
                        }
                    }
                }
                return t;
            }

            // 为属性或字段设值
            void set(String alias, Object value) throws Exception {
                if (value == null) return;
                try {
                    Method setter = wrap.getMethod("set" + (alias.substring(0,1).toUpperCase() + alias.substring(1)), value.getClass());
                    if (setter != null) {
                        setter.setAccessible(true);
                        setter.invoke(t, value);
                        return;
                    }
                } catch (NoSuchMethodException e) {/** ignore **/}
                try {
                    Field field = wrap.getField(alias);
                    if (field != null) {
                        field.setAccessible(true);
                        field.set(t, value);
                    }
                } catch (NoSuchFieldException e) {/** ignore **/}
            }
        };
    }


    /**
     * 查询全部数据
     * @param eType 实体类型
     * @return 全部数据 {@link List<E>}
     */
    public <E extends IEntity> List<E> findAll(Class<E> eType) {
        return findList(eType, null, null, null);
    }


    /**
     * 查询多条数据
     * @param eType 实体类型
     * @param spec 条件
     * @return 多个实体 {@link List<E>}
     */
    public <E extends IEntity> List<E> findList(Class<E> eType, CriteriaSpec<E> spec) {
        return findList(eType, null, null, spec);
    }


    /**
     * 查询多条数据
     * @param eType 实体类型
     * @param start 开始行 从0开始
     * @param limit 条数限制
     * @return 多个实体 {@link List<E>}
     */
    public <E extends IEntity> List<E> findList(Class<E> eType, Integer start, Integer limit) {
        return findList(eType, start, limit, null);
    }


    /**
     * 查询多条数据
     * @param eType 实体类型
     * @param start 开始行 从0开始
     * @param limit 条数限制
     * @param spec 条件
     * @return 多个实体 {@link List<E>}
     */
    public <E extends IEntity> List<E> findList(Class<E> eType, Integer start, Integer limit, CriteriaSpec<E> spec) {
        if (eType == null) throw new IllegalArgumentException("Param eType required");
        if (start != null && start < 0) throw new IllegalArgumentException("Param start >= 0 or not give");
        if (limit != null && limit <= 0) throw new IllegalArgumentException("Param limit must > 0 or not give");
        return trans(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<E> cQuery = cb.createQuery(eType);
            Root<E> root = cQuery.from(eType);
            Object p = spec == null ? null : spec.toPredicate(root, cQuery, cb);
            if (p instanceof Predicate) cQuery.where((Predicate) p);
            Query<E> query = session.createQuery(cQuery);
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
     * @param <E> {@link IEntity}
     * @return 一页实体 {@link Page<E>}
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
     * @return 一页实体 {@link Page<E>}
     */
    public <E extends IEntity> Page<E> findPage(Class<E> eType, Integer page, Integer pageSize, CriteriaSpec<E> spec) {
        if (eType == null) throw new IllegalArgumentException("Param eType required");
        if (page == null || page < 1) throw new IllegalArgumentException("Param page >=1");
        if (pageSize == null || pageSize < 1) throw new IllegalArgumentException("Param pageSize >=1");
        return trans(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<E> query = cb.createQuery(eType);
            Root<E> root = query.from(eType);
            Object p = spec == null ? null : spec.toPredicate(root, query, cb);
            if (p instanceof Predicate) query.where((Predicate) p);
            return new Page<E>().setPage(page).setPageSize(pageSize)
                    .setList(session.createQuery(query).setFirstResult((page - 1) * pageSize).setMaxResults(pageSize).list())
                    .setTotalRow(count(eType, spec));
        });
    }


    /**
     * 统计某张表总数
     * @param eType 实体类型
     * @param <E> {@link IEntity}
     * @return 条数
     */
    public <E extends IEntity> long count(Class<E> eType) { return count(eType, null); }


    /**
     * 根据实体类, 统计
     * @param eType 实体类型
     * @param spec 条件
     * @return 条数
     */
    public <E extends IEntity> long count(Class<E> eType, CriteriaSpec<E> spec) {
        if (eType == null) throw new IllegalArgumentException("Param eType required");
        return trans(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Long> query = cb.createQuery(Long.class);
            Root<E> root = query.from(eType);
            Object p = spec == null ? null : spec.toPredicate(root, query, cb);
            if (query.isDistinct()) query.select(cb.countDistinct(root));
            else query.select(cb.count(root));
            query.orderBy(Collections.emptyList());
            if (p instanceof Predicate) query.where((Predicate) p);
            return ((Number) session.createQuery(query).getSingleResult()).longValue();
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
    public static SessionFactory createSessionFactory(DataSource datasource, Map props, List<Class<? extends IEntity>> entities) {
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
        if (ds != null) return ds;

        // Hikari 数据源
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
        if (ds != null) return ds;

        // dbcp2 数据源
        try {
            Properties props = new Properties();
            dsAttr.forEach((s, o) -> props.put(s, Objects.toString(o, "")));
            // if (!props.containsKey("validationQuery")) props.put("validationQuery", "select 1");
            ds = (DataSource) Class.forName("org.apache.commons.dbcp2.BasicDataSourceFactory").getMethod("createDataSource", Properties.class).invoke(null, props);
        }
        catch(ClassNotFoundException ex) {}
        catch(Exception ex) { throw new RuntimeException(ex); }

        if (ds == null) throw new RuntimeException("No found DataSource impl class");
        return ds;
    }
}