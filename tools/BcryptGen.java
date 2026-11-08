import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 生成 bcrypt 哈希（仅开发/运维重置用，不参与业务）。
 *
 * <pre>
 * java -cp %USERPROFILE%\.m2\repository\org\springframework\security\spring-security-crypto\6.3.4\spring-security-crypto-6.3.4.jar ^
 *      tools\BcryptGen.java "Admin@12345"
 * </pre>
 */
public final class BcryptGen {

    private BcryptGen() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("用法: BcryptGen <明文密码>");
            System.exit(2);
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(args[0]);
        System.out.println(hash);
        System.out.println("VERIFY=" + encoder.matches(args[0], hash));
    }
}
