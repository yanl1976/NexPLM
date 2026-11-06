-- =====================================================================
-- P7-5 工时填报与审批 + 资源负荷（简化）
--
-- 粒度：一条 = 某人某天在某任务上的一段工时。同一天同一任务只允许一条
--       （uk 保证），多段工作写进备注——按天记才是「实际发生了什么」，
--       按「工时块」记会让审批列表膨胀十倍，且没人核对得完。
--
-- 状态机：DRAFT（可改可删）→(submit) SUBMITTED →(approve) APPROVED
--                                        ↓(reject)
--                                    REJECTED（可改后重新提交）
--   只有 DRAFT/REJECTED 能改能删；APPROVED 是事实记录，改了就等于篡改考勤。
--
-- project_id 冗余存一份（由服务端从 task 反查写入）：任务不允许跨项目移动，
--       所以不会漂移；换来的是项目级汇总/过滤不用每次都 JOIN prj_task。
--
-- 工时口径：项目实际工时只认 APPROVED；SUBMITTED 单独统计为「待审批」，
--       便于「已填未审」不要被当成已经发生。
--
-- 资源负荷（简化版）= 某人某周 APPROVED 工时 / (工作日 × 8h)，
--       工作日按 5 天算（40h/周），不建节假日日历——真正的日历排程在 P7-7。
-- =====================================================================

CREATE TABLE prj_timesheet (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    project_id    bigint       NOT NULL,
    task_id       bigint       NOT NULL,
    user_id       bigint       NOT NULL,                      -- 填报人
    work_date     date         NOT NULL,
    hours         numeric(5,2) NOT NULL,                      -- 0.5 ~ 24
    work_type     varchar(16)  NOT NULL DEFAULT 'NORMAL',     -- NORMAL 正常 / OVERTIME 加班
    status        varchar(16)  NOT NULL DEFAULT 'DRAFT',
                                                              -- DRAFT / SUBMITTED / APPROVED / REJECTED
    remark        varchar(500),
    submitted_at  timestamptz,
    approved_by   bigint,
    approved_at   timestamptz,
    reject_reason varchar(500),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_prj_timesheet ON prj_timesheet (task_id, user_id, work_date) WHERE deleted = false;
CREATE INDEX idx_prj_timesheet_user ON prj_timesheet (company_id, user_id, work_date DESC) WHERE deleted = false;
CREATE INDEX idx_prj_timesheet_project
    ON prj_timesheet (company_id, project_id, status, work_date) WHERE deleted = false;
CREATE INDEX idx_prj_timesheet_task ON prj_timesheet (task_id) WHERE deleted = false;
