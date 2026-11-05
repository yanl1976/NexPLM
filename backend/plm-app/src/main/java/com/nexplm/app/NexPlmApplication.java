package com.nexplm.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * NexPLM 启动类。
 * <ul>
 *   <li>scanBasePackages=com.nexplm：聚合 common / platform / app 三个模块</li>
 *   <li>MapperScan：统一扫描各模块 mapper 包</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.nexplm")
@MapperScan("com.nexplm.**.mapper")
@EnableScheduling
public class NexPlmApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexPlmApplication.class, args);
    }
}
