import cn.xnatural.jpa.Repo;
import entity.Db;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JpaTest {

    protected static final Logger log = LoggerFactory.getLogger(JpaTest.class);

    public static void main(String[] args) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("jdbcUrl", "jdbc:mysql://localhost:3306/mysql?user=root&password=root");
        attrs.put("hibernate.hbm2ddl.auto", "update");
        Repo repo = new Repo("testRepo", attrs).init();
    }


    @Test
    void testGetDbName() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?user=root&password=root").init();
        log.info(repo.getDbName());
        repo.close();
    }


    @Test
    void testGetJdbcUrl() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?user=root&password=root").init();
        log.info(repo.getJdbcUrl());
        repo.close();
    }


    @Test
    void testEntity() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?user=root&password=root").entities(Db.class).init();
        log.info(repo.find(Db.class, (root, query, cb) -> cb.equal(root.get("Db"), "sys")).Host);
        repo.close();
    }


    @Test
    void testEntityPage() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?user=root&password=root").entities(Db.class).init();
        log.info(repo.findPage(Db.class, 1, 10, (root, query, cb) -> cb.equal(root.get("Db"), "sys")).toString());
        repo.close();
    }


    @Test
    void testSqlFirstRow() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?user=root&password=root").init();
        log.info(repo.firstRow("select count(1) as total from db").get("total").toString());
        repo.close();
    }


    @Test
    void testSqlFirstRowWithParam() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?user=root&password=root").init();
        log.info(repo.firstRow("select * from db where Db=?", "sys").toString());
        repo.close();
    }


    @Test
    void testSqlRows() {
        Repo repo = new Repo("jdbc:mysql://localhost:3306/mysql?user=root&password=root").init();
        log.info(repo.rows("select * from db").toString());
        repo.close();
    }
}
