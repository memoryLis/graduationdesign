package com.hmall.item.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.item.domain.dto.ItemCommentFormDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemComment;
import com.hmall.item.domain.vo.ItemCommentVO;
import com.hmall.item.mapper.ItemCommentMapper;
import com.hmall.item.service.IItemCommentService;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemCommentServiceImpl extends ServiceImpl<ItemCommentMapper, ItemComment> implements IItemCommentService {

    private final IItemService itemService;

    @Override
    public PageDTO<ItemCommentVO> queryCommentsByItemId(Long itemId, PageQuery query) {
        if (itemId == null) {
            throw new BadRequestException("商品id不能为空");
        }
        Page<ItemComment> page = lambdaQuery()
                .eq(ItemComment::getItemId, itemId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        if (CollUtils.isEmpty(page.getRecords())) {
            return PageDTO.empty(page);
        }
        return PageDTO.of(page, fillCommentVO(page.getRecords(), false));
    }

    @Override
    public void saveMyComment(ItemCommentFormDTO dto) {
        if (dto == null || dto.getItemId() == null) {
            throw new BadRequestException("商品id不能为空");
        }
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BadRequestException("请先登录");
        }
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty()) {
            throw new BadRequestException("评论内容不能为空");
        }
        if (content.length() > 300) {
            throw new BadRequestException("评论内容不能超过300字");
        }
        Item item = itemService.getById(dto.getItemId());
        if (item == null) {
            throw new BadRequestException("商品不存在");
        }
        ItemComment comment = new ItemComment();
        comment.setUserId(userId);
        comment.setItemId(dto.getItemId());
        comment.setContent(content);
        save(comment);

        itemService.lambdaUpdate()
                .eq(Item::getId, dto.getItemId())
                .setSql("comment_count = IFNULL(comment_count, 0) + 1")
                .update();
    }

    @Override
    public PageDTO<ItemCommentVO> queryMyComments(PageQuery query) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BadRequestException("请先登录");
        }
        Page<ItemComment> page = lambdaQuery()
                .eq(ItemComment::getUserId, userId)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        if (CollUtils.isEmpty(page.getRecords())) {
            return PageDTO.empty(page);
        }
        return PageDTO.of(page, fillCommentVO(page.getRecords(), true));
    }

    @Override
    public void deleteMyComment(Long commentId) {
        if (commentId == null) {
            throw new BadRequestException("评论id不能为空");
        }
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BadRequestException("请先登录");
        }
        ItemComment comment = getById(commentId);
        if (comment == null) {
            throw new BadRequestException("评论不存在");
        }
        if (!userId.equals(comment.getUserId())) {
            throw new BadRequestException("只能删除自己的评论");
        }
        removeById(commentId);
        itemService.lambdaUpdate()
                .eq(Item::getId, comment.getItemId())
                .setSql("comment_count = IF(IFNULL(comment_count, 0) > 0, comment_count - 1, 0)")
                .update();
    }

    private List<ItemCommentVO> fillCommentVO(List<ItemComment> comments, boolean withItemName) {
        Map<Long, String> itemMap = CollUtils.emptyMap();
        if (withItemName) {
            Set<Long> itemIds = comments.stream().map(ItemComment::getItemId).collect(Collectors.toSet());
            List<ItemDTO> items = itemService.queryItemByIds(itemIds);
            itemMap = items.stream().collect(Collectors.toMap(ItemDTO::getId, ItemDTO::getName, (a, b) -> a));
        }

        Map<Long, String> finalItemMap = itemMap;
        return BeanUtils.copyList(comments, ItemCommentVO.class, (comment, vo) -> {
            vo.setUsername("用户" + comment.getUserId());
            if (withItemName) {
                vo.setItemName(finalItemMap.getOrDefault(comment.getItemId(), "商品" + comment.getItemId()));
            }
        });
    }
}
