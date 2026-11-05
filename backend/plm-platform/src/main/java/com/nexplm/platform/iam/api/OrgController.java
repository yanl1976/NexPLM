package com.nexplm.platform.iam.api;

import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.iam.service.OrgService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统-组织架构")
@RestController
@RequestMapping("/api/v1/system/orgs")
@PreAuthorize("hasAuthority('system.org.manage')")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @Operation(summary = "组织树")
    @GetMapping
    public Result<List<IamDtos.OrgNode>> tree() {
        return Result.ok(orgService.tree(CurrentUser.companyId()));
    }

    @Operation(summary = "新建组织")
    @PostMapping
    @Audit(module = "IAM", action = "ORG_CREATE", objectType = "SysOrg")
    public Result<Long> create(@Valid @RequestBody IamDtos.OrgSaveReq req) {
        return Result.ok(orgService.create(CurrentUser.companyId(), req));
    }

    @Operation(summary = "修改组织")
    @PutMapping("/{id}")
    @Audit(module = "IAM", action = "ORG_UPDATE", objectType = "SysOrg")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody IamDtos.OrgSaveReq req) {
        orgService.update(id, req);
        return Result.ok();
    }

    @Operation(summary = "删除组织")
    @DeleteMapping("/{id}")
    @Audit(module = "IAM", action = "ORG_DELETE", objectType = "SysOrg")
    public Result<Void> delete(@PathVariable Long id) {
        orgService.delete(id);
        return Result.ok();
    }
}
