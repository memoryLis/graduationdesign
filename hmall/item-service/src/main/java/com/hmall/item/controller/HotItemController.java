package com.hmall.item.controller;

import com.hmall.common.domain.PageDTO;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("hotItem")
@RequiredArgsConstructor
public class HotItemController {

    public static final String HOT_ITEM_KEY = "hot_items";
    public static final String HOT_ITEM_STOCK_KEY_PREFIX = "hot_item_stock:";
    private static final Integer ITEM_STATUS_NORMAL = 1;

    private final IItemService itemService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("listHotItems")
    public PageDTO<ItemDTO> queryHotItemByPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        List<Object> hotItemValues = redisTemplate.opsForHash().values(HOT_ITEM_KEY);
        if (hotItemValues == null || hotItemValues.isEmpty()) {
            return new PageDTO<>(0L, 0L, Collections.emptyList());
        }

        List<ItemDTO> hotItems = new ArrayList<>();
        for (Object value : hotItemValues) {
            if (value instanceof ItemDTO) {
                ItemDTO dto = (ItemDTO) value;
                if (ITEM_STATUS_NORMAL.equals(dto.getStatus())) {
                    dto.setHot(Boolean.TRUE);
                    hotItems.add(dto);
                }
            }
        }
        if (hotItems.isEmpty()) {
            return new PageDTO<>(0L, 0L, Collections.emptyList());
        }

        hotItems = hotItems.stream()
                .sorted(Comparator.comparing(ItemDTO::getId).reversed())
                .collect(Collectors.toList());

        long total = hotItems.size();
        long pages = (total + size - 1) / size;
        int fromIndex = Math.toIntExact(Math.max(0, (current - 1) * size));
        if (fromIndex >= hotItems.size()) {
            return new PageDTO<>(total, pages, Collections.emptyList());
        }
        int toIndex = Math.min(hotItems.size(), Math.toIntExact(fromIndex + size));
        return new PageDTO<>(total, pages, hotItems.subList(fromIndex, toIndex));
    }
}
