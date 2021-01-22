import cn.xnatural.jpa.Repo;
import entity.Db;
import entity.User;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JpaTest {

    protected static final Logger log = LoggerFactory.getLogger(JpaTest.class);

    public static void main(String[] args) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("jdbcUrl", "jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root");
        attrs.put("hibernate.hbm2ddl.auto", "update"); //update: 自动根据实体更新表结构, none: 不更新
        Repo repo = new Repo(attrs).entities(Db.class).init();
        repo.close();
    }


    @Test
    void testGetDbName() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").init();
        log.info(repo.getDbName());
        repo.close();
    }


    @Test
    void testGetJdbcUrl() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").init();
        log.info(repo.getJdbcUrl());
        repo.close();
    }


    @Test
    void testEntity() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").entities(Db.class).init();
        log.info(repo.find(Db.class, (root, query, cb) -> cb.equal(root.get("Db"), "sys")).Host);
        repo.close();
    }


    @Test
    void testEntityPage() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").entities(Db.class).init();
        log.info(repo.findPage(Db.class, 1, 10, (root, query, cb) -> cb.equal(root.get("Db"), "sys")).toString());
        repo.close();
    }


    @Test
    void testHqlFirstRow() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").entities(Db.class).init();
        log.info(repo.hqlFirstRow("select count(1) as total from Db", Long.class).toString());
        repo.close();
    }

    @Test
    void testHqlFirstRow2() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").entities(User.class).init();
        Map record = repo.hqlFirstRow("select new map(Host as host,User as user) from User where User=:user", Map.class, "root");
        log.info(record == null ? "" : record.toString());
        repo.close();
    }

    @Test
    void testHqlFirstRow3() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").entities(User.class).init();
        User record = repo.hqlFirstRow("select new entity.User(Host,User) from User where User=:user", User.class, "root");
        log.info(record == null ? "" : record.toString());
        repo.close();
    }


    @Test
    void testSqlFirstRow() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").init();
        log.info(repo.firstRow("select count(1) as total from db").get("total").toString());
        repo.close();
    }


    @Test
    void testSqlFirstRowWithParam() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").init();
        log.info(repo.firstRow("select * from db where Db=?",  Db.class,"sys").toString());
        repo.close();
    }


    @Test
    void testSqlRows() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").init();
        log.info(repo.rows("select * from db where Db=?", "sys").toString());
        repo.close();
    }


    @Test
    void testSqlRowsWithWrap() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").init();
        log.info(repo.rows("select * from db where Db=?", Db.class, "sys").toString());
        repo.close();
    }


    @Test
    void testSqlPage() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").init();
        log.info(repo.sqlPage("select * from db where Db=?", 1, 10, Db.class, "sys").toString());
        repo.close();
    }


    @Test
    void testSqlIn() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root").init();
        log.info(repo.rows("select * from db where Db = :db and Db in (:ids)", "sys", Arrays.asList("sys")).toString());
//        log.info(repo.rows("select * from db where Db in (:ids)",  Arrays.asList("sys", "xx")).toString());
//         log.info(repo.rows("select * from db where Db in (?)", Arrays.asList("sys")).toString());
//        log.info(repo.rows("select * from db where Db = ?", "sys").toString());
        repo.close();
    }


    @Test
    void testUpdate() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root").init();
        log.info(repo.execute("update test set age=? where id=?", 11, "4028b881766f3e5801766f3e87ba0000") + "");
        repo.close();
    }


    @Test
    void testInsert() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root").init();
        log.info(repo.execute("insert into test values(?,?,?,?,?)",
                UUID.randomUUID().toString().replace("-", ""), new Date(), new Date(), 22, "name"
                ) + "");
        repo.close();
    }


    @Test
    void testDelete() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root").init();
        log.info(repo.execute("delete from test where id=?", "ad3e4ff8f3fd4171aeeb9dd2c0aa6f0c") + "");
        repo.close();
    }
}
