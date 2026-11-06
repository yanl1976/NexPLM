package com.nexplm.platform.workflow.service.assignee;

import org.springframework.stereotype.Component;

import java.util.List;

/** 发起人：取流程发起人本人（退回补充、发起人自审等场景）。 */
@Component
public class StarterAssigneeResolver implements AssigneeResolver {

    @Override
    public String type() {
        return "STARTER";
    }

    @Override
    public String label() {
        return "发起人";
    }

    @Override
    public List<Long> resolve(AssigneeSpec spec, AssigneeContext context) {
        return context == null || context.starterId() == null ? List.of() : List.of(context.starterId());
    }
}
