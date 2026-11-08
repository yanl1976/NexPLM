import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 执行一个 .sql 文件（用于 deploy/migration 下的迁移草稿）。
 *
 * <p>本机没有 psql，故用 JDBC 的 simple query 一次性执行整份文件（PG JDBC 支持多语句）。
 * 连接信息从环境变量读，与后端启动口径一致：</p>
 *
 * <pre>
 * NEXPLM_DB_URL=jdbc:postgresql://localhost:5432/postgres
 * NEXPLM_DB_USER=postgres
 * NEXPLM_DB_PASSWORD=postgres
 * java -cp &lt;pg-jdbc.jar&gt; tools/RunSqlFile.java deploy/migration/010-doc_class.sql
 * </pre>
 */
public final class RunSqlFile {

    private RunSqlFile() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("用法：java RunSqlFile.java <file.sql>");
            System.exit(2);
        }
        String url = env("NEXPLM_DB_URL", "jdbc:postgresql://localhost:5432/postgres");
        String user = env("NEXPLM_DB_USER", "postgres");
        String password = env("NEXPLM_DB_PASSWORD", "postgres");
        String sql = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        System.out.println("EXECUTED " + args[0] + " chars=" + sql.length());
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
