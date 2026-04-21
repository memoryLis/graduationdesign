package com.hmall.item.controller;

import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.item.domain.dto.ItemCommentFormDTO;
import com.hmall.item.domain.vo.ItemCommentVO;
import com.hmall.item.service.IItemCommentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "商品评论相关接口")
@RestController
@RequestMapping("/item-comments")
@RequiredArgsConstructor
public class ItemCommentController {

    private final IItemCommentService itemCommentService;

    @ApiOperation("查询商品评论列表")
    @GetMapping("/item/{itemId}")
    public PageDTO<ItemCommentVO> queryCommentsByItemId(@PathVariable("itemId") Long itemId, PageQuery query) {
        return itemCommentService.queryCommentsByItemId(itemId, query);
    }

    @ApiOperation("发表评论")
    @PostMapping
    public void saveMyComment(@RequestBody ItemCommentFormDTO dto) {
        itemCommentService.saveMyComment(dto);
    }

    @ApiOperation("查询我的评论列表")
    @GetMapping("/me")
    public PageDTO<ItemCommentVO> queryMyComments(PageQuery query) {
        return itemCommentService.queryMyComments(query);
    }

    @ApiOperation("删除我的评论")
    @DeleteMapping("/{commentId}")
    public void deleteMyComment(@PathVariable("commentId") Long commentId) {
        itemCommentService.deleteMyComment(commentId);
    }
}
