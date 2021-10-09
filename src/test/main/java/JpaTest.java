import cn.xnatural.jpa.Repo;
import entity.Db;
import entity.TestUUIDEntity;
import entity.User;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class JpaTest {

    protected static final Logger log = LoggerFactory.getLogger(JpaTest.class);

    public static void main(String[] args) {

    }


    @Test
    void testCreate() {
        try(Repo repo = new Repo("jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true")
                .setAttr("hibernate.hbm2ddl.auto", "update")
                .entities(Db.class).init()) {
        }
    }


    @Test
    void testGetDbName() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.getDbName());
        }
    }


    @Test
    void testGetDialect() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&allowPublicKeyRetrieval=true", "root", "root").init()) {
            log.info(repo.getDialect());
        }
    }


    @Test
    void testGetDBVersion() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.getDBVersion());
        }
    }


    @Test
    void testGetJdbcUrl() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.getJdbcUrl());
        }
    }


    @Test
    void testUUID() {
        try (
                Repo repo = new Repo("jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true")
                        .setAttr("hibernate.hbm2ddl.auto", "update") //update: 自动根据实体更新表结构, none: 不更新
                        .entities(TestUUIDEntity.class).init()
        ) {
            TestUUIDEntity entity = new TestUUIDEntity();
            entity.setName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            repo.saveOrUpdate(entity);
            log.info(entity.getId());
        }
    }


    @Test
    void testEntity() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").entities(Db.class).init()) {
            log.info(repo.find(Db.class, (root, query, cb) -> cb.equal(root.get("Db"), "sys")).Host);
        }
    }


    @Test
    void testEntityPage() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").entities(Db.class).init()) {
            log.info(repo.findPage(Db.class, 1, 10, (root, query, cb) -> cb.equal(root.get("Db"), "sys")).toString());
        }
    }


    @Test
    void testHqlFirstRow() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").entities(Db.class).init()) {
            log.info(repo.hqlFirstRow("select count(1) as total from Db", Long.class).toString());
        }
    }

    @Test
    void testHqlFirstRow2() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").entities(User.class).init()) {
            Map record = repo.hqlFirstRow("select new map(Host as host,User as user) from User where User=:user", Map.class, "root");
            log.info(record == null ? "" : record.toString());
        }
    }

    @Test
    void testHqlFirstRow3() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").entities(User.class).init()) {
            User user = repo.hqlFirstRow("select new entity.User(Host,User) from User where User=:user", User.class, "root");
            log.info(user == null ? "" : user.toString());
        }
    }


    @Test
    void testHqlRows() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").entities(Db.class).init()) {
            log.info(repo.hqlRows("select count(1) as total from Db", Long.class).toString());
        }
    }

    @Test
    void testHqlRows2() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").entities(User.class).init()) {
            List<Map> records = repo.hqlRows("select new map(Host as host,User as user) from User where User=:user", Map.class, "root");
            log.info(records.toString());
        }
    }

    @Test
    void testHqlRows3() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").entities(User.class).init()) {
            List<User> users = repo.hqlRows("select new entity.User(Host,User) from User where User=:user", User.class, "root");
            log.info(users.toString());
        }
    }


    @Test
    void testSqlFirstRow() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.firstRow("select count(1) as total from db").get("total").toString());
        }
    }


    @Test
    void testSqlFirstRowWithParam() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.firstRow("select * from db where Db=?",  Db.class,"sys").toString());
        }
    }


    @Test
    void testSqlRows() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.rows("select * from db where Db=?", "sys").toString());
        }
    }


    @Test
    void testSqlRowsWithWrap() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.rows("select * from db where Db=?", Db.class, "sys").toString());
        }
    }


    @Test
    void testSqlPage() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.sqlPage("select * from db where Db=?", 1, 10, Db.class, "sys").toString());
        }
    }


    @Test
    void testSqlIn() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true").init()) {
            log.info(repo.rows("select * from db where Db = :db and Db in (:ids)", "sys", Arrays.asList("sys")).toString());
//        log.info(repo.rows("select * from db where Db in (:ids)",  Arrays.asList("sys", "xx")).toString());
//         log.info(repo.rows("select * from db where Db in (?)", Arrays.asList("sys")).toString());
//        log.info(repo.rows("select * from db where Db = ?", "sys").toString());
        }
    }


    @Test
    void testInsert() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true")
                .entities(entity.Test.class)
                .setAttr("hibernate.hbm2ddl.auto", "update")
                .init()) {
            log.info(repo.execute("insert into test(create_time, update_time, age, name) values(?,?,?,?)", new Date(), new Date(), 22, "name") + "");
        }
    }


    @Test
    void testUpdate() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true")
                .entities(entity.Test.class)
                .setAttr("hibernate.hbm2ddl.auto", "update")
                .init()) {
            log.info(repo.execute("update test set age=? where id=?", 11, 1) + "");
        }
    }


    @Test
    void testDelete() {
        try (Repo repo = new Repo("jdbc:mysql://localhost:3306/test?useSSL=false&user=root&password=root&allowPublicKeyRetrieval=true")
                .entities(entity.Test.class)
                .setAttr("hibernate.hbm2ddl.auto", "update")
                .init()) {
            log.info(repo.execute("delete from test where id=?", 1) + "");
        }
    }
}
