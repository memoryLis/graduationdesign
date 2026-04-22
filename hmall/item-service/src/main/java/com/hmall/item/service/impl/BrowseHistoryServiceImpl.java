package com.hmall.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.domain.PageDTO;
import com.hmall.item.domain.po.BrowseHistory;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.vo.BrowseHistoryVO;
import com.hmall.item.mapper.BrowseHistoryMapper;
import com.hmall.item.service.IBrowseHistoryService;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl extends ServiceImpl<BrowseHistoryMapper, BrowseHistory> implements IBrowseHistoryService {

    private final IItemService itemService;

    @Override
    public void recordBrowse(Long userId, Long itemId) {
        LambdaQueryWrapper<BrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BrowseHistory::getUserId, userId)
               .eq(BrowseHistory::getItemId, itemId);

        BrowseHistory existing = getOne(wrapper);
        if (existing != null) {
            existing.setUpdateTime(LocalDateTime.now());
            updateById(existing);
        } else {
            BrowseHistory history = new BrowseHistory();
            history.setUserId(userId);
            history.setItemId(itemId);
            save(history);
        }
    }

    @Override
    public PageDTO<BrowseHistoryVO> getMyBrowseHistory(Long userId, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<BrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BrowseHistory::getUserId, userId)
               .orderByDesc(BrowseHistory::getUpdateTime);

        Page<BrowseHistory> page = page(new Page<>(pageNo, pageSize), wrapper);

        if (page.getRecords().isEmpty()) {
            return new PageDTO<>(0L, 0L, new ArrayList<>());
        }

        List<Long> itemIds = page.getRecords().stream()
                .map(BrowseHistory::getItemId)
                .collect(Collectors.toList());

        List<Item> items = itemService.listByIds(itemIds);
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, item -> item));

        List<BrowseHistoryVO> voList = page.getRecords().stream()
                .map(history -> {
                    BrowseHistoryVO vo = new BrowseHistoryVO();
                    vo.setId(history.getId());
                    vo.setUserId(history.getUserId());
                    vo.setItemId(history.getItemId());
                    vo.setCreateTime(history.getCreateTime());

                    Item item = itemMap.get(history.getItemId());
                    if (item != null) {
                        vo.setItemName(item.getName());
                        vo.setItemImage(item.getImage());
                        vo.setItemPrice(item.getPrice());
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageDTO<>(page.getTotal(), page.getPages(), voList);
    }
}
