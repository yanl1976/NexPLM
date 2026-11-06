package com.nexplm.platform.notify.channel;

import java.util.List;
import java.util.Map;

/**
 * 通知通道 SPI：新增一种通道（短信、飞书、Teams…）只需实现本接口并声明为 Bean，
 * 业务侧（{@code NotifyService}）按 {@link #type()} 路由，不需要改分发逻辑。
 *
 * <p>约定：</p>
 * <ul>
 *   <li>{@link #validate} 只做结构与地址校验，不连网——保存配置时不该因为对端暂时不可达而失败；</li>
 *   <li>{@link #send} 不抛异常，失败以 {@link SendResult} 返回：失败要落消息记录并进重试，
 *       抛异常会把"发送失败"变成"接口 500"，用户看不到真实原因。</li>
 *   <li>{@link #secretFields()} 声明配置里的敏感字段，详情接口据此抹值（只回"已配置"）。</li>
 * </ul>
 */
public interface NotifySender {

    /** 通道类型：EMAIL / WECOM / DINGTALK。 */
    String type();

    void validate(Map<String, Object> config);

    SendResult send(Map<String, Object> config, String title, String content);

    default List<String> secretFields() {
        return List.of();
    }

    /** 发送结果：{@code response} 留痕（对端回执片段），{@code error} 为空表示成功。 */
    record SendResult(boolean success, String response, String error) {

        public static SendResult ok(String response) {
            return new SendResult(true, response, null);
        }

        public static SendResult fail(String error) {
            return new SendResult(false, null, error);
        }
    }
}
