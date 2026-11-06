-- 变更执行并入生命周期唯一写入口。
--
-- 原 ChgExecService 对 RELEASE / OBSOLETE 直接 UPDATE plm_object.lifecycle_state，
-- 绕过了 LifecycleService.transition：不校验迁移、不写 plm_state_log、不走乐观锁。
-- 这里给默认生命周期补两条「变更执行」动作，让变更执行也能走同一写入口：
--   Draft --release-->  Released  变更发布
--   Draft --obsolete--> Obsolete  变更废弃
--
-- 审批路径不变：Draft --submit--> InWorkflow --approve--> Released；
-- 已发布后的废弃仍是 Released --obsolete--> Obsolete。
--
-- 守卫：仅当定义里还没有该迁移时才追加，避免覆盖设计器里的自定义改动。

UPDATE meta_lifecycle
   SET definition_json = jsonb_set(
           definition_json,
           '{transitions}',
           COALESCE(definition_json -> 'transitions', '[]'::jsonb)
             || '[{"from":"Draft","to":"Released","action":"release","label":"变更发布"}]'::jsonb),
       version_no = version_no + 1,
       updated_at = now()
 WHERE code = 'DEFAULT_LIFECYCLE'
   AND NOT EXISTS (
       SELECT 1
         FROM jsonb_array_elements(COALESCE(definition_json -> 'transitions', '[]'::jsonb)) AS t
        WHERE t ->> 'from' = 'Draft' AND t ->> 'to' = 'Released' AND t ->> 'action' = 'release');

UPDATE meta_lifecycle
   SET definition_json = jsonb_set(
           definition_json,
           '{transitions}',
           COALESCE(definition_json -> 'transitions', '[]'::jsonb)
             || '[{"from":"Draft","to":"Obsolete","action":"obsolete","label":"变更废弃"}]'::jsonb),
       version_no = version_no + 1,
       updated_at = now()
 WHERE code = 'DEFAULT_LIFECYCLE'
   AND NOT EXISTS (
       SELECT 1
         FROM jsonb_array_elements(COALESCE(definition_json -> 'transitions', '[]'::jsonb)) AS t
        WHERE t ->> 'from' = 'Draft' AND t ->> 'to' = 'Obsolete' AND t ->> 'action' = 'obsolete');
