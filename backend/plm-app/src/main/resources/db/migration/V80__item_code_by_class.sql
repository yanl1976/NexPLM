-- =====================================================================
-- 零部件编码：按分类取号（复刻蓝本编码体系，EXP-20261108-43）
--
-- 蓝本真相（只读取证）：
--   CODERES 里 OBJECTTYPE='零组件' 的记录形如 CODES=10010100001、ORDINAL=00001、
--   PROPERTY='ECODE'、GENERATORPATH=种类ID;分类链路…；即
--   **编码 = 对象所属分类的编码（动态前缀） + 该分类内独立递增的 5 位流水**。
--   16,343 条零件-分类关系中 15,476 条（94.7%）的零件编码以分类码开头；
--   每个分类组都从 xxxxxx00001 起（如 10180800001、12040500001）。
--
-- 新系统此前只有 PdmItem（固定前缀 PN-），与蓝本口径不符，故新增本规则：
--   CLASS 段 = 分类编码；SEQ(5) = 分类内独立流水（由 code_sequence 按 scope_key 计数）。
-- 无分类的对象仍走 PdmItem（PN-），不强行要求归类。
-- =====================================================================

INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status, version_no,
                       created_at, updated_at, current_no, last_period, object_type, remark)
VALUES (930101, 'PdmItemByClass', '零部件编码（分类码 + 分类内流水）',
        '[{"type":"CLASS"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb,
        '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, NULL, 'PdmItem',
        'EXP-20261108-43：复刻蓝本 CODERES（100101 + 00001）；流水按分类独立计数')
ON CONFLICT DO NOTHING;
