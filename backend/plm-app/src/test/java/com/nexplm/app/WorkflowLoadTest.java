package com.nexplm.app;

import com.nexplm.platform.workflow.api.WfDtos;
import com.nexplm.platform.workflow.service.WorkflowService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P4 闸口压测：1000 个并行审批任务（默认 125 实例 × 8 会签人）。
 *
 * <p>默认跳过，手动执行：</p>
 * <pre>
 * mvn -pl plm-app -am test -Dtest=WorkflowLoadTest -Dnexplm.perf=true
 * # 可调规模：-Dnexplm.perf.instances=200（会签人数固定 8）
 * </pre>
 *
 * <p>压测直接打 Service 层而非 HTTP：MockMvc 的 DispatcherServlet 不适合多线程复用，
 * 真正要压的是流程引擎 + 数据库事务，Service 层才是这两者的真实入口。</p>
 *
 * <p>并发办理同一实例的多个会签任务可能因引擎乐观锁（ACT_RU_EXECUTION.REV_）冲突失败，
 * 这是引擎的正常行为，压测允许一次重试并单独统计重试率。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "nexplm.perf", matches = "true")
class WorkflowLoadTest {

    private static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException ex) {
            throw new IllegalStateException("启动测试环境失败", ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("nexplm.security.bootstrap.admin-password", () -> "Admin@12345");
    }

    private static final int INSTANCES = Integer.getInteger("nexplm.perf.instances", 125);
    private static final int COUNTERSIGNERS = 8;
    private static final int THREADS = 32;

    private final AtomicInteger RETRIES = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private TaskService taskService;

    @Test
    void oneThousandConcurrentTasks() throws Exception {
        String admin = login("admin", "Admin@12345");
        long adminId = currentUserId(admin);

        List<Long> countersigners = new ArrayList<>();
        for (int i = 0; i < COUNTERSIGNERS; i++) {
            countersigners.add(createUser(admin, "perf_user_" + i, "压测会签" + i));
        }

        long t0 = System.currentTimeMillis();
        List<Long> itemIds = new ArrayList<>();
        for (int i = 0; i < INSTANCES; i++) {
            itemIds.add(createItem(admin, "压测零件" + i));
        }
        long createMs = System.currentTimeMillis() - t0;

        t0 = System.currentTimeMillis();
        List<String> initialTaskIds = new ArrayList<>();
        List<String> pids = new ArrayList<>();
        Map<String, Long> assigneeOfTask = new HashMap<>();
        for (Long itemId : itemIds) {
            WfDtos.StartResp started = workflowService.start(new WfDtos.StartReq(
                    "PdmItem", itemId, null, adminId, countersigners,
                    null, null, null, "压测发起"), adminId);
            pids.add(started.processInstanceId());
            String taskId = started.activeTasks().get(0).taskId();
            initialTaskIds.add(taskId);
            assigneeOfTask.put(taskId, adminId);
        }
        long startMs = System.currentTimeMillis() - t0;

        t0 = System.currentTimeMillis();
        int initialFailures = runConcurrently(initialTaskIds,
                id -> workflowService.complete(id, new WfDtos.CompleteReq("初审通过", null), adminId));
        long initialMs = System.currentTimeMillis() - t0;

        List<Task> countersignTasks = taskService.createTaskQuery()
                .taskDefinitionKey("countersign")
                .processInstanceIdIn(pids)
                .list();
        int expectedCountersign = INSTANCES * COUNTERSIGNERS;
        assertThat(countersignTasks).hasSize(expectedCountersign);
        List<String> countersignTaskIds = new ArrayList<>();
        for (Task task : countersignTasks) {
            countersignTaskIds.add(task.getId());
            assigneeOfTask.put(task.getId(), Long.valueOf(task.getAssignee()));
        }

        t0 = System.currentTimeMillis();
        int countersignFailures = runConcurrently(countersignTaskIds, id -> workflowService.complete(
                id, new WfDtos.CompleteReq("会签通过", null), assigneeOfTask.get(id)));
        long countersignMs = System.currentTimeMillis() - t0;

        long remaining = taskService.createTaskQuery().processInstanceIdIn(pids).count();

        System.out.printf("[PERF] instances=%d countersigners=%d tasks=%d%n",
                INSTANCES, COUNTERSIGNERS, initialTaskIds.size() + countersignTaskIds.size());
        System.out.printf("[PERF] createItem=%dms start=%dms initialComplete=%dms countersignComplete=%dms%n",
                createMs, startMs, initialMs, countersignMs);
        System.out.printf("[PERF] failures(initial=%d, countersign=%d) retries=%d remainingTasks=%d%n",
                initialFailures, countersignFailures, RETRIES.get(), remaining);

        assertThat(initialFailures).isZero();
        assertThat(countersignFailures).isZero();
        assertThat(remaining).isZero();
    }

    /** 并发执行，失败重试一次（应对引擎乐观锁），返回最终失败数。 */
    private int runConcurrently(List<String> ids, Consumer<String> action) {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        AtomicInteger failures = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (String id : ids) {
            futures.add(pool.submit(() -> {
                try {
                    action.accept(id);
                } catch (Exception first) {
                    RETRIES.incrementAndGet();
                    try {
                        action.accept(id);
                    } catch (Exception second) {
                        failures.incrementAndGet();
                        System.err.println("[PERF] task " + id + " failed: " + second.getMessage());
                    }
                }
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception ignored) {
                failures.incrementAndGet();
            }
        }
        pool.shutdown();
        return failures.get();
    }

    // ------------------------------------------------------------ 工具（HTTP 仅用于造基础数据）

    private long createUser(String adminToken, String username, String displayName) throws Exception {
        call(post("/api/v1/system/users"), adminToken, json(Map.of(
                "username", username,
                "password", "Passw0rd!",
                "displayName", displayName,
                "securityLevel", 1,
                "roleIds", new long[]{businessRoleId(adminToken)})));
        JsonNodeUtil users = new JsonNodeUtil(
                call(get("/api/v1/system/users?page=1&size=100"), adminToken, null).path("items"));
        return users.idOf("username", username);
    }

    private long businessRoleId(String adminToken) throws Exception {
        JsonNodeUtil roles = new JsonNodeUtil(call(get("/api/v1/system/roles"), adminToken, null));
        return roles.idOf("code", "BUSINESS");
    }

    private long createItem(String token, String name) throws Exception {
        return call(post("/api/v1/items"), token, json(Map.of(
                "name", name, "itemType", "PART", "sourceType", "MAKE", "unit", "EA"))).path("id").asLong();
    }

    private long currentUserId(String token) throws Exception {
        return call(get("/api/v1/auth/me"), token, null).path("id").asLong();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).path("data").path("accessToken").asText();
    }

    private com.fasterxml.jackson.databind.JsonNode call(MockHttpServletRequestBuilder builder,
                                                         String token, String payload) throws Exception {
        builder.header("Authorization", "Bearer " + token);
        if (payload != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(payload);
        }
        MvcResult result = mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
        com.fasterxml.jackson.databind.JsonNode root = body(result);
        return root.has("data") ? root.path("data") : root;
    }

    private com.fasterxml.jackson.databind.JsonNode body(MvcResult result) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String json(Object value) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
    }

    /** 简单包装：按字段值查 id，避免重复遍历。 */
    private record JsonNodeUtil(com.fasterxml.jackson.databind.JsonNode node) {
        long idOf(String field, String value) {
            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                if (value.equals(item.path(field).asText())) {
                    return item.path("id").asLong();
                }
            }
            throw new IllegalStateException("未找到 " + field + "=" + value);
        }
    }
}
