package com.nexplm.platform.workflow.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.workflow.api.WfDtos;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 流程定义管理：列表、XML 读取、部署新版本（bpmn-js 设计器保存走这里）。
 *
 * <p>版本语义由 Flowable 保证：同一 {@code process id}（即 processKey）重复部署自动 +1 版本，
 * 已在运行的实例继续走启动时的旧版本，新发起的实例走最新版本。</p>
 */
@Service
public class WorkflowDefinitionService {

    /** 资源名需以 {@code .bpmn20.xml} 结尾，Flowable 才按 BPMN 解析。 */
    private static final String RESOURCE_SUFFIX = ".bpmn20.xml";

    private final RepositoryService repositoryService;

    public WorkflowDefinitionService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /** 每个 processKey 的最新版本。 */
    public List<WfDtos.DefinitionRow> list() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .orderByProcessDefinitionKey().asc()
                .list()
                .stream()
                .map(this::toRow)
                .toList();
    }

    /** 读取某个定义的 BPMN XML。 */
    public WfDtos.DefinitionXml xml(String definitionId) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
        if (definition == null) {
            throw BizException.of(ErrorCode.WF_DEFINITION_NOT_FOUND, "流程定义不存在：" + definitionId);
        }
        try (InputStream in = repositoryService.getResourceAsStream(
                definition.getDeploymentId(), definition.getResourceName())) {
            if (in == null) {
                throw BizException.of(ErrorCode.WF_DEFINITION_NOT_FOUND,
                        "流程定义资源不存在：" + definition.getResourceName());
            }
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new WfDtos.DefinitionXml(definition.getId(), definition.getKey(),
                    definition.getName(), definition.getVersion(), xml);
        } catch (IOException ex) {
            throw BizException.of(ErrorCode.WF_DEFINITION_NOT_FOUND, "读取流程 XML 失败：" + ex.getMessage());
        }
    }

    /**
     * 部署（或升级）一个流程定义。
     * <p>解析失败（XML 语法错误、缺少 startEvent 等）由引擎抛出并被包装，
     * 这里统一转成 400 交给前端展示，而不是 500。</p>
     */
    public WfDtos.DefinitionRow deploy(WfDtos.DeployReq req) {
        String resourceName = "process-" + System.currentTimeMillis() + RESOURCE_SUFFIX;
        Deployment deployment;
        try {
            deployment = repositoryService.createDeployment()
                    .name(req.name())
                    .addString(resourceName, req.bpmnXml())
                    .deploy();
        } catch (Exception ex) {
            // 引擎解析失败可能被 Spring 事务拦截器包装成 TransactionSystemException，
            // 故不限于 FlowableException；取根因消息回给前端（部署纯属用户输入驱动，统一 400）。
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "流程定义部署失败：" + rootMessage(ex));
        }
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId()).list();
        if (definitions.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST,
                    "XML 中未找到流程定义（需要 <process id=\"...\">）");
        }
        return toRow(definitions.get(0));
    }

    /** 部署时间在 Deployment 上，ProcessDefinition 本身不带，需回查一次（定义数量少，不做批量优化）。 */
    private WfDtos.DefinitionRow toRow(ProcessDefinition definition) {
        Deployment deployment = repositoryService.createDeploymentQuery()
                .deploymentId(definition.getDeploymentId()).singleResult();
        return new WfDtos.DefinitionRow(definition.getId(), definition.getKey(), definition.getName(),
                definition.getVersion(), definition.getDeploymentId(),
                deployment == null ? null : toOffsetDateTime(deployment.getDeploymentTime()),
                definition.getResourceName());
    }

    private static OffsetDateTime toOffsetDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    /** 取最内层异常消息，避免只看到「Transaction rolled back」这类包装文案。 */
    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }
}
