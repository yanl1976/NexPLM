package com.nexplm.app.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花 ID 超出 JS 安全整数范围（2^53），统一把 Long 序列化为字符串，避免前端精度丢失。
 * 方案约定：所有 ID 对外一律字符串。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule longToStringModule() {
        SimpleModule module = new SimpleModule();
        // 仅包装类型 Long（ID、外键）转字符串；原始类型 long（total、expiresIn 等计数/时长）保持数字
        module.addSerializer(Long.class, ToStringSerializer.instance);
        return module;
    }
}
