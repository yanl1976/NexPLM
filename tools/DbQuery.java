import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * 本机 PostgreSQL 是 zonky 缓存的精简版（只有服务端与 pg_ctl，没有 psql/createdb），
 * 用它代行一次性 SQL 查询与数据修正，避免为了看一张表去装客户端。
 *
 * <p>用法（JDK 17 单文件源码模式，driver 用 maven 本地仓库里的那个）：</p>
 * <pre>
 * java -cp %USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar ^
 *      tools\DbQuery.java "select count(*) from doc_preview"
 * </pre>
 *
 * <p>连接参数可用环境变量覆盖：PGHOST / PGPORT / PGDATABASE / PGUSER / PGPASSWORD，
 * 默认 localhost:5432/postgres，用户 postgres/postgres（本机开发库）。</p>
 */
public final class DbQuery {

    private DbQuery() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("用法: java -cp <postgresql-driver.jar> tools/DbQuery.java \"<SQL>\" [\"<SQL2>\" ...]");
            System.exit(2);
        }
        String url = "jdbc:postgresql://" + env("PGHOST", "localhost") + ":" + env("PGPORT", "5432")
                + "/" + env("PGDATABASE", "postgres");
        try (Connection conn = DriverManager.getConnection(url, env("PGUSER", "postgres"),
                env("PGPASSWORD", "postgres"))) {
            for (String sql : args) {
                System.out.println("-- " + sql);
                try (Statement statement = conn.createStatement()) {
                    if (statement.execute(sql)) {
                        try (ResultSet rs = statement.getResultSet()) {
                            print(rs);
                        }
                    } else {
                        System.out.println("OK rows=" + statement.getUpdateCount());
                    }
                }
            }
        }
    }

    private static void print(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();
        while (rs.next()) {
            StringBuilder line = new StringBuilder();
            for (int i = 1; i <= columns; i++) {
                if (i > 1) {
                    line.append(" | ");
                }
                line.append(meta.getColumnLabel(i)).append('=').append(rs.getString(i));
            }
            System.out.println(line);
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
