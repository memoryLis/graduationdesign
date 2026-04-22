package com.hmall.item.controller;

import com.hmall.common.domain.PageDTO;
import com.hmall.common.utils.UserContext;
import com.hmall.item.domain.vo.BrowseHistoryVO;
import com.hmall.item.service.IBrowseHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "浏览历史相关接口")
@RestController
@RequestMapping("/browse-history")
@RequiredArgsConstructor
public class BrowseHistoryController {

    private final IBrowseHistoryService browseHistoryService;

    @ApiOperation("记录浏览历史")
    @PostMapping("/record/{itemId}")
    public void recordBrowse(@PathVariable Long itemId) {
        Long userId = UserContext.getUser();
        browseHistoryService.recordBrowse(userId, itemId);
    }

    @ApiOperation("获取我的浏览历史")
    @GetMapping("/my")
    public PageDTO<BrowseHistoryVO> getMyBrowseHistory(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getUser();
        return browseHistoryService.getMyBrowseHistory(userId, pageNo, pageSize);
    }
}
