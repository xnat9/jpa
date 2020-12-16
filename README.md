## 介绍
jpa: 封装hibernate

## 安装教程
```
<dependency>
    <groupId>cn.xnatural.jpa</groupId>
    <artifactId>jpa</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 用法
### 创建
```
// 根据jdbcUrl 创建
Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?user=root&password=root").init();
```
```
// 根据属性集创建
Map<String, Object> attrs = new HashMap<>();
attrs.put("jdbcUrl", "jdbc:mysql://localhost:3306/mysql?user=root&password=root");
attrs.put("hibernate.hbm2ddl.auto", "update"); //自动更新表结构
Repo repo = new Repo("testRepo", attrs).entities(Db.class).init();
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
repo.findPage(Db.class, 1, 10, (root, query, cb) -> cb.equal(root.get("Db"), "sys"));
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
```

#### 查询多条数据
```
List<Map<String, Object>> results = repo.rows("select * from db limit 10")
```

### 自定义操作
```
repo.trans(session -> {
    // TODO
    return null;
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