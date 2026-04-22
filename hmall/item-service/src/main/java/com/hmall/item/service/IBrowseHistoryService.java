package com.hmall.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.PageDTO;
import com.hmall.item.domain.po.BrowseHistory;
import com.hmall.item.domain.vo.BrowseHistoryVO;

public interface IBrowseHistoryService extends IService<BrowseHistory> {

    void recordBrowse(Long userId, Long itemId);

    PageDTO<BrowseHistoryVO> getMyBrowseHistory(Long userId, Integer pageNo, Integer pageSize);
}
