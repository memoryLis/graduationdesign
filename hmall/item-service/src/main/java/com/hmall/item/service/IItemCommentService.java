package com.hmall.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.item.domain.dto.ItemCommentFormDTO;
import com.hmall.item.domain.po.ItemComment;
import com.hmall.item.domain.vo.ItemCommentVO;

public interface IItemCommentService extends IService<ItemComment> {

    PageDTO<ItemCommentVO> queryCommentsByItemId(Long itemId, PageQuery query);

    void saveMyComment(ItemCommentFormDTO dto);

    PageDTO<ItemCommentVO> queryMyComments(PageQuery query);

    void deleteMyComment(Long commentId);
}
