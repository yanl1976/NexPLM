package com.nexplm.platform.object.api;

import com.nexplm.common.api.Result;
import com.nexplm.platform.object.service.ObjectLookupService;
import com.nexplm.platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * P10-3 扫码查对象：编码精确匹配。
 *
 * <p>权限用"任一域可读"而不是新开权限点：扫码只是移动端进入详情的一个入口，
 * 能看物料/文档/变更的人本来就该能扫到对应对象；新开权限点只会让管理员多配一次。</p>
 */
@Tag(name = "对象-扫码查询")
@RestController
@RequestMapping("/api/v1/objects")
public class ObjectLookupController {

    private final ObjectLookupService objectLookupService;

    public ObjectLookupController(ObjectLookupService objectLookupService) {
        this.objectLookupService = objectLookupService;
    }

    @Operation(summary = "按编码精确查对象（扫码用，查不到返回空数组）")
    @GetMapping("/lookup")
    @PreAuthorize("hasAnyAuthority('pse.item.read', 'pdm.doc.read', 'chg.ecr.read')")
    public Result<List<ObjectLookupDtos.LookupResult>> lookup(@RequestParam String code) {
        return Result.ok(objectLookupService.lookup(CurrentUser.companyId(), code));
    }
}
