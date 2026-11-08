package com.nexplm.platform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** HTTPS 判定与重定向地址的纯函数单测（不发请求，避免依赖 spring-test）。 */
class HttpsEnforcementFilterTest {

    @Test
    void secureDetectionTrustsForwardedProto() {
        assertThat(HttpsEnforcementFilter.isSecure(true, null)).isTrue();
        assertThat(HttpsEnforcementFilter.isSecure(false, "https")).isTrue();
        // 头值大小写不敏感（nginx 常写 HTTPS）
        assertThat(HttpsEnforcementFilter.isSecure(false, "HTTPS")).isTrue();
        assertThat(HttpsEnforcementFilter.isSecure(false, "http")).isFalse();
        assertThat(HttpsEnforcementFilter.isSecure(false, null)).isFalse();
    }

    @Test
    void probeEndpointsAreSkipped() {
        assertThat(HttpsEnforcementFilter.isSkipped("/actuator/health")).isTrue();
        assertThat(HttpsEnforcementFilter.isSkipped("/actuator/health/liveness")).isTrue();
        assertThat(HttpsEnforcementFilter.isSkipped("/actuator/info")).isTrue();
        assertThat(HttpsEnforcementFilter.isSkipped("/api/v1/docs")).isFalse();
    }

    @Test
    void redirectTargetKeepsPathAndQuery() {
        assertThat(HttpsEnforcementFilter.httpsUrl("plm.example.com", null,
                "/api/v1/docs", "page=1"))
                .isEqualTo("https://plm.example.com/api/v1/docs?page=1");
        // 外部域名优先（前置 nginx 改写 Host）
        assertThat(HttpsEnforcementFilter.httpsUrl("10.0.0.5:8080", "plm.example.com",
                "/doc/1", null))
                .isEqualTo("https://plm.example.com/doc/1");
        assertThat(HttpsEnforcementFilter.httpsUrl("plm.example.com", null, "/", ""))
                .isEqualTo("https://plm.example.com/");
    }
}
