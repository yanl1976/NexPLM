-- 文档详情补「可视化」页签
-- 背景：SView 插件（旧系统 Server/Base/Ctrls/SView，HTML5 三维轻量化查看器）已迁移到前端 public/sview，
--      后端新增 GET /visual/docs/{id} 判定查看器类型，文档详情因此具备与旧系统一致的「可视化」页签。
-- 位置：排在「文件」(20) 之后、「历史版本」(30) 之前。
INSERT INTO meta_tab (id, entity_code, code, name, tab_type, permission_code, sort) VALUES
    (901008, 'PdmDoc', 'visual', '可视化', 'VISUAL', 'pdm.doc.read', 25)
ON CONFLICT (id) DO NOTHING;
