-- =====================================================================
-- P5：变更申请接入工作流审批
--   ECR 提交 → 发起 nexplmApproval（初审=接收人，会签=接收人单人）
--   流程归档 → 插件回写 ECR = APPROVED；撤回 → 回写 DRAFT
-- =====================================================================
INSERT INTO wf_binding (id, object_type, category, condition_json, process_key, name, priority, enabled)
VALUES (150911, 'ChgEcr', NULL, '{}'::jsonb, 'nexplmApproval', '变更申请通用审批', 10, true)
ON CONFLICT DO NOTHING;
