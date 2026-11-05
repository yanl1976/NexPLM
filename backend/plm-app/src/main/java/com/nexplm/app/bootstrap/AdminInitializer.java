package com.nexplm.app.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexplm.platform.iam.domain.SysUser;
import com.nexplm.platform.iam.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 首次启动创建管理员账号并绑定系统管理员角色。
 * 密码来自配置（NEXPLM_ADMIN_PASSWORD），且强制首次登录改密。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(SysUserMapper userMapper,
                            PasswordEncoder passwordEncoder,
                            JdbcTemplate jdbcTemplate,
                            @Value("${nexplm.security.bootstrap.admin-username:admin}") String adminUsername,
                            @Value("${nexplm.security.bootstrap.admin-password:Admin@12345}") String adminPassword) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, adminUsername));
        if (exists != null && exists > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setCompanyId(1L);
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setDisplayName("系统管理员");
        admin.setSecurityLevel(9);
        admin.setStatus("ACTIVE");
        admin.setMustChangePwd(true);
        admin.setPwdUpdatedAt(OffsetDateTime.now());
        admin.setAuthProvider("LOCAL");
        admin.setVersion(0);
        admin.setDeleted(false);
        userMapper.insert(admin);

        Integer roleId = jdbcTemplate.query("SELECT id FROM sys_role WHERE company_id = 1 AND code = 'SYS_ADMIN'",
                rs -> rs.next() ? rs.getInt(1) : null);
        if (roleId != null) {
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    admin.getId(), roleId.longValue());
        }
        log.warn("已创建初始管理员账号 [{}]，请立即登录并修改密码（配置项 NEXPLM_ADMIN_PASSWORD）", adminUsername);
    }
}
