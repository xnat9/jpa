## 介绍
jpa: 封装hibernate

## 安装教程
```
<dependency>
    <groupId>cn.xnatural.jpa</groupId>
    <artifactId>jpa</artifactId>
    <version>1.0.2</version>
</dependency>
```

## 用法
### 创建
```
// 根据jdbcUrl 创建
Repo repo = new Repo("jdbc:mysql://localhost:3306/test?user=root&password=root").init();
```
```
// 根据属性集创建
Map<String, Object> attrs = new HashMap<>();
attrs.put("jdbcUrl", "jdbc:mysql://localhost:3306/test?user=root&password=root");
attrs.put("hibernate.hbm2ddl.auto", "update"); //update: 自动根据实体更新表结构, none: 不更新
Repo repo = new Repo(attrs).entities(Db.class).init();
```
### 实体查询
```
@Entity
@Table(name = "Db")
public class Db implements IEntity {
    @Id
    public String Db;
    public String Host;
}
```
#### 查询一个实体
```
Db db = repo.find(Db.class, (root, query, cb) -> cb.equal(root.get("Db"), "sys"));
```

#### 分页查询实体
```
Page<Db> pageData = repo.findPage(Db.class, 1, 10, (root, query, cb) -> cb.equal(root.get("Db"), "sys"));
```

#### 其它实体方法
```
// 保存或更新实体
repo.saveOrUpdate(实体对象)
// 根据id查询实体
repo.findById(实体Class, id值)
// 删除一个实体
repo.delete(实体对象)
// 查询多个实体
repo.findList(实体Class, 条件)
// 统计实体个数
repo.count(实体Class, 条件(可选))
```

### 原生sql操作
#### 查询一条数据
```
// 1. 无参
repo.firstRow("select count(1) as total from db").get("total")
// 2. 传参
Map<String, Object> result = repo.firstRow("select * from db where Db=?", "sys");
// 3. 包装结果
Db result = repo.firstRow("select * from db where Db=?", Db.class, "sys");
```

#### 查询多条数据
```
// 1. 默认返回List<Map>
List<Map<String, Object>> results = repo.rows("select * from db limit ?", 10);
// 2. 指定返回结果
List<Db> results = repo.rows("select * from db where Db=?", Db.class, "sys");
// 3. 分页查询
Page<Db> pageData = repo.sqlPage("select * from db where Db=?", 1, 10, Db.class, "sys");
// 4. 命名参数
List<Map<String, Object>> results = repo.rows("select * from db where Db = :db and Db in (:ids)", "sys", Arrays.asList("sys"));
```

#### 更新,插入,删除
```
1. 更新
repo.execute("update test set age=? where id=?", 11, "4028b881766f3e5801766f3e87ba0000")
2. 插入
repo.execute("insert into test values(?,?,?,?,?)", UUID.randomUUID().toString().replace("-", ""), new Date(), new Date(), 22, "name")
3. 删除
repo.execute("delete from test where id=?", "ad3e4ff8f3fd4171aeeb9dd2c0aa6f0c")
```

### 自定义操作
```
// 1. 其它自定义查询
repo.trans(session -> {
    // TODO
    // session.createQuery(hql);// hql查询
    return null;
})
// 2. 事务成功/失败回调
repo.trans(session -> {
    // TODO
    // session.createQuery(hql);// hql查询
    return null;
}, () -> {
    // TODO 成功执行
}, (ex) -> {
    // TODO 失败执行
})
```

### 其它方法
```
// 查询实体映射的表名
repo.tbName(实体Class);
// 得到当前的连接jdbcUrl
repo.getJdbcUrl();
// 得到当前连接的数据库名(mysql)
repo.getDbName();
```

## 参与贡献
xnatural@msn.cn
