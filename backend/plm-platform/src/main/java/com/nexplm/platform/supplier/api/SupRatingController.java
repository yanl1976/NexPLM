package com.nexplm.platform.supplier.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.security.CurrentUser;
import com.nexplm.platform.supplier.service.SupplierRatingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 供应商评级：维度可配方案 + 评级记录（P8-5-1）。 */
@Tag(name = "供应商-评级")
@RestController
@RequestMapping("/api/v1")
public class SupRatingController {

    private final SupplierRatingService ratingService;

    public SupRatingController(SupplierRatingService ratingService) {
        this.ratingService = ratingService;
    }

    @Operation(summary = "评级方案列表")
    @GetMapping("/sup/rating-schemes")
    @PreAuthorize("hasAuthority('sup.rating.read')")
    public Result<List<SupRatingDtos.SchemeRow>> schemes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return Result.ok(ratingService.schemes(CurrentUser.companyId(), category, status));
    }

    @Operation(summary = "评级方案详情（含维度与等级规则）")
    @GetMapping("/sup/rating-schemes/{id}")
    @PreAuthorize("hasAuthority('sup.rating.read')")
    public Result<SupRatingDtos.SchemeDetail> scheme(@PathVariable Long id) {
        return Result.ok(ratingService.schemeDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建评级方案（自动取号 RATE-xxxxxx）")
    @PostMapping("/sup/rating-schemes")
    @PreAuthorize("hasAuthority('sup.rating.write')")
    @Audit(module = "SUP", action = "RATING_SCHEME_CREATE", objectType = "SupRateScheme")
    public Result<SupRatingDtos.SchemeDetail> createScheme(
            @Valid @RequestBody SupRatingDtos.SchemeSaveReq req) {
        return Result.ok(ratingService.createScheme(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改评级方案（维度整表替换，历史评级有快照）")
    @PutMapping("/sup/rating-schemes/{id}")
    @PreAuthorize("hasAuthority('sup.rating.write')")
    @Audit(module = "SUP", action = "RATING_SCHEME_UPDATE", objectType = "SupRateScheme")
    public Result<SupRatingDtos.SchemeDetail> updateScheme(
            @PathVariable Long id, @Valid @RequestBody SupRatingDtos.SchemeSaveReq req) {
        return Result.ok(ratingService.updateScheme(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除评级方案（已有评级记录时拒绝）")
    @DeleteMapping("/sup/rating-schemes/{id}")
    @PreAuthorize("hasAuthority('sup.rating.write')")
    @Audit(module = "SUP", action = "RATING_SCHEME_DELETE", objectType = "SupRateScheme")
    public Result<Void> deleteScheme(@PathVariable Long id) {
        ratingService.deleteScheme(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "评级记录列表（可按供应商/方案/周期/状态过滤）")
    @GetMapping("/sup/ratings")
    @PreAuthorize("hasAuthority('sup.rating.read')")
    public Result<PageResult<SupRatingDtos.RatingRow>> ratings(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long schemeId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(ratingService.ratings(CurrentUser.companyId(), supplierId, schemeId,
                period, status, page, size));
    }

    @Operation(summary = "评级详情（含维度快照打分）")
    @GetMapping("/sup/ratings/{id}")
    @PreAuthorize("hasAuthority('sup.rating.read')")
    public Result<SupRatingDtos.RatingDetail> rating(@PathVariable Long id) {
        return Result.ok(ratingService.ratingDetail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建评级（按方案快照维度打分，生成草稿）")
    @PostMapping("/sup/ratings")
    @PreAuthorize("hasAuthority('sup.rating.write')")
    @Audit(module = "SUP", action = "RATING_CREATE", objectType = "SupRate")
    public Result<SupRatingDtos.RatingDetail> createRating(
            @Valid @RequestBody SupRatingDtos.RatingSaveReq req) {
        return Result.ok(ratingService.createRating(req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "修改评级草稿（重新打分并重算总分）")
    @PutMapping("/sup/ratings/{id}")
    @PreAuthorize("hasAuthority('sup.rating.write')")
    @Audit(module = "SUP", action = "RATING_UPDATE", objectType = "SupRate")
    public Result<SupRatingDtos.RatingDetail> updateRating(
            @PathVariable Long id, @Valid @RequestBody SupRatingDtos.RatingSaveReq req) {
        return Result.ok(ratingService.updateRating(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "确认评级（回写供应商等级/分数，达到暂停档则暂停供应商）")
    @PostMapping("/sup/ratings/{id}/confirm")
    @PreAuthorize("hasAuthority('sup.rating.write')")
    @Audit(module = "SUP", action = "RATING_CONFIRM", objectType = "SupRate")
    public Result<SupRatingDtos.RatingDetail> confirmRating(@PathVariable Long id) {
        return Result.ok(ratingService.confirmRating(id, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除评级草稿（已确认的不可删）")
    @DeleteMapping("/sup/ratings/{id}")
    @PreAuthorize("hasAuthority('sup.rating.write')")
    @Audit(module = "SUP", action = "RATING_DELETE", objectType = "SupRate")
    public Result<Void> deleteRating(@PathVariable Long id) {
        ratingService.deleteRating(id, CurrentUser.companyId());
        return Result.ok(null);
    }

    @Operation(summary = "某供应商的全部评级记录")
    @GetMapping("/sup/suppliers/{id}/ratings")
    @PreAuthorize("hasAuthority('sup.rating.read')")
    public Result<List<SupRatingDtos.RatingRow>> supplierRatings(@PathVariable Long id) {
        return Result.ok(ratingService.ratingsBySupplier(CurrentUser.companyId(), id));
    }
}
