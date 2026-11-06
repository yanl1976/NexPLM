package com.nexplm.platform.analysis.api;

import com.nexplm.common.api.PageResult;
import com.nexplm.common.api.Result;
import com.nexplm.common.audit.Audit;
import com.nexplm.platform.analysis.service.BoardService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * P10-2 自定义看板：看板 CRUD + 数据集字典 + 预览数据。
 *
 * <p>看板只存"用哪个数据集、怎么展示"，数据集是服务端白名单，
 * 因此接口没有、也不可能接受任意 SQL。</p>
 */
@Tag(name = "分析-自定义看板")
@RestController
@RequestMapping("/api/v1/ana/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @Operation(summary = "可用数据集字典（白名单）")
    @GetMapping("/datasets")
    @PreAuthorize("hasAuthority('ana.board.read')")
    public Result<List<BoardDtos.DatasetMeta>> datasets() {
        return Result.ok(boardService.datasets());
    }

    @Operation(summary = "看板列表")
    @GetMapping
    @PreAuthorize("hasAuthority('ana.board.read')")
    public Result<PageResult<BoardDtos.BoardRow>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(boardService.page(CurrentUser.companyId(), keyword, page, size));
    }

    @Operation(summary = "看板详情（含组件布局）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ana.board.read')")
    public Result<BoardDtos.BoardDetail> detail(@PathVariable Long id) {
        return Result.ok(boardService.detail(id, CurrentUser.companyId()));
    }

    @Operation(summary = "新建看板")
    @PostMapping
    @PreAuthorize("hasAuthority('ana.board.write')")
    @Audit(module = "ANA", action = "BOARD_CREATE", objectType = "AnaBoard")
    public Result<BoardDtos.BoardDetail> create(@Valid @RequestBody BoardDtos.BoardSaveReq req) {
        return Result.ok(boardService.create(req, CurrentUser.companyId(), CurrentUser.userId()));
    }

    @Operation(summary = "修改看板（整体覆盖布局）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ana.board.write')")
    @Audit(module = "ANA", action = "BOARD_UPDATE", objectType = "AnaBoard")
    public Result<BoardDtos.BoardDetail> update(@PathVariable Long id,
                                                @Valid @RequestBody BoardDtos.BoardSaveReq req) {
        return Result.ok(boardService.update(id, req, CurrentUser.companyId(),
                CurrentUser.userId()));
    }

    @Operation(summary = "删除看板（软删）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ana.board.write')")
    @Audit(module = "ANA", action = "BOARD_DELETE", objectType = "AnaBoard")
    public Result<Void> delete(@PathVariable Long id) {
        boardService.delete(id, CurrentUser.companyId(), CurrentUser.userId());
        return Result.ok(null);
    }

    @Operation(summary = "预览数据（按组件顺序返回各自数据集结果）")
    @GetMapping("/{id}/data")
    @PreAuthorize("hasAuthority('ana.board.read')")
    public Result<BoardDtos.BoardData> data(@PathVariable Long id) {
        return Result.ok(boardService.data(id, CurrentUser.companyId()));
    }
}
