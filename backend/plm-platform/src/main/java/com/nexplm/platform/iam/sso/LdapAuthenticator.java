package com.nexplm.platform.iam.sso;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;

/**
 * LDAP / AD 校验：服务账号查人 → 用本人 DN 再 bind 一次验证密码。
 *
 * <p>两个坑必须避开：</p>
 * <ol>
 *   <li><b>空密码</b>：很多 LDAP 在密码为空时会退化成匿名 bind 并"成功"，
 *       等于任意账号零密码登录。这里先拒绝空密码。</li>
 *   <li><b>过滤串注入</b>：用户名直接拼进 filter 会被 {@code *)(uid=*} 这类输入绕过，
 *       必须按 RFC 4515 转义后再拼。</li>
 * </ol>
 */
@Component
public class LdapAuthenticator {

    private static final int TIMEOUT_MS = 6000;

    public record Config(String url, String baseDn, String bindDn, String bindCredential,
                         String userFilter, String userDnPattern, String attrUsername,
                         String attrDisplayName, String attrEmail, String subjectAttr) {
    }

    public record Identity(String subject, String username, String displayName, String email) {
    }

    public Identity authenticate(Config config, String username, String password) {
        if (password == null || password.isEmpty()) {
            // 空密码在 LDAP 里可能变成匿名 bind，必须挡在门外
            throw BizException.of(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        if (username == null || username.isBlank()) {
            throw BizException.of(ErrorCode.AUTH_BAD_CREDENTIALS);
        }
        String userDn;
        String attrUsernameValue = null;
        String displayName = null;
        String email = null;
        String subject = null;

        if (notBlank(config.userDnPattern())) {
            // 直绑模式：DN 模板里替换 {0}，不做搜索
            userDn = config.userDnPattern().replace("{0}", escapeFilter(username));
            attrUsernameValue = username;
        } else {
            Hashtable<String, String> env = baseEnv(config);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            if (notBlank(config.bindDn())) {
                env.put(Context.SECURITY_PRINCIPAL, config.bindDn());
                env.put(Context.SECURITY_CREDENTIALS, nullToEmpty(config.bindCredential()));
            }
            String filter = config.userFilter().replace("{0}", escapeFilter(username));
            DirContext context = null;
            try {
                context = new InitialDirContext(env);
                SearchControls controls = new SearchControls();
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                controls.setCountLimit(2);
                String[] attrs = new String[]{config.attrUsername(), config.attrDisplayName(),
                        config.attrEmail(), config.subjectAttr()};
                NamingEnumeration<SearchResult> results = context.search(config.baseDn(), filter,
                        controls);
                if (!results.hasMore()) {
                    throw BizException.of(ErrorCode.AUTH_BAD_CREDENTIALS);
                }
                SearchResult result = results.next();
                userDn = result.getNameInNamespace();
                Attributes found = result.getAttributes();
                attrUsernameValue = attrValue(found, config.attrUsername());
                displayName = attrValue(found, config.attrDisplayName());
                email = attrValue(found, config.attrEmail());
                subject = attrValue(found, config.subjectAttr());
            } catch (BizException ex) {
                throw ex;
            } catch (NamingException ex) {
                throw BizException.of(ErrorCode.SSO_HANDSHAKE_FAILED,
                        "目录服务查询失败：" + ex.getMessage());
            } finally {
                closeQuietly(context);
            }
        }

        Hashtable<String, String> userEnv = baseEnv(config);
        userEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        userEnv.put(Context.SECURITY_PRINCIPAL, userDn);
        userEnv.put(Context.SECURITY_CREDENTIALS, password);
        DirContext userContext = null;
        try {
            userContext = new InitialDirContext(userEnv);
        } catch (NamingException ex) {
            // 密码错与账号不存在都归为凭据错误，避免把目录结构暴露给调用方
            throw BizException.of(ErrorCode.AUTH_BAD_CREDENTIALS);
        } finally {
            closeQuietly(userContext);
        }

        String resolvedUsername = notBlank(attrUsernameValue) ? attrUsernameValue : username;
        String resolvedSubject = notBlank(subject) ? subject : userDn;
        return new Identity(resolvedSubject, resolvedUsername,
                notBlank(displayName) ? displayName : resolvedUsername, email);
    }

    private Hashtable<String, String> baseEnv(Config config) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.url());
        env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(TIMEOUT_MS));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(TIMEOUT_MS));
        env.put(Context.REFERRAL, "follow");
        return env;
    }

    private static String attrValue(Attributes attributes, String name) throws NamingException {
        if (!notBlank(name) || attributes == null) {
            return null;
        }
        var attr = attributes.get(name);
        return attr == null ? null : String.valueOf(attr.get());
    }

    /** RFC 4515 过滤值转义。 */
    static String escapeFilter(String value) {
        StringBuilder sb = new StringBuilder();
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\u0000' -> sb.append("\\00");
                default -> sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static void closeQuietly(DirContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (NamingException ignored) {
                // 关闭失败不影响认证结论
            }
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
