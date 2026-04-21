package com.hmall.item.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("items")
public class ItemManageController {

    private static final Integer ITEM_STATUS_NORMAL = 1;

    @Autowired
    private IItemService itemService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("manage/add")
    public String addItem(ItemDTO itemdto, @RequestParam MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        String image = saveImage(file);
        Item item = new Item();
        BeanUtils.copyProperties(itemdto, item);
        item.setImage(image);
        itemService.save(item);
        return "success";
    }

    @PutMapping("manage/update")
    public String updateItem(ItemDTO itemDTO, @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (itemDTO.getId() == null) {
            throw new RuntimeException("商品id不能为空");
        }
        Item item = new Item();
        BeanUtils.copyProperties(itemDTO, item);

        if (file != null && !file.isEmpty()) {
            item.setImage(saveImage(file));
        }

        itemService.updateById(item);
        syncHotItemCache(itemDTO.getId());
        return "success";
    }

    @DeleteMapping("manage/delete/{id}")
    public String deleteItem(@PathVariable Long id) {
        itemService.removeById(id);
        redisTemplate.opsForHash().delete(HotItemController.HOT_ITEM_KEY, String.valueOf(id));
        redisTemplate.delete(HotItemController.HOT_ITEM_STOCK_KEY_PREFIX + id);
        return "success";
    }

    @GetMapping("manage/list")
    public PageDTO<ItemDTO> listItems(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(required = false) String name) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Item::getName, name);
        }
        wrapper.orderByDesc(Item::getUpdateTime);

        Page<Item> page = itemService.page(new Page<>(pageNo, pageSize), wrapper);
        Set<Object> hotItemKeys = redisTemplate.opsForHash().keys(HotItemController.HOT_ITEM_KEY);
        Set<String> safeHotItemIds = hotItemKeys == null ? Collections.emptySet()
                : hotItemKeys.stream().map(String::valueOf).collect(Collectors.toSet());

        return PageDTO.of(page, item -> {
            ItemDTO dto = BeanUtils.copyBean(item, ItemDTO.class);
            dto.setHot(safeHotItemIds.contains(String.valueOf(item.getId())));
            return dto;
        });
    }

    @PostMapping("manage/addHotItem")
    public String addHotItem(@RequestParam Long itemId) {
        Item hotItem = itemService.getById(itemId);
        if (hotItem == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!ITEM_STATUS_NORMAL.equals(hotItem.getStatus())) {
            throw new RuntimeException("只有上架商品才能设为热门商品");
        }
        redisTemplate.opsForHash().put(HotItemController.HOT_ITEM_KEY, String.valueOf(itemId), toHotItemDTO(hotItem));
        redisTemplate.opsForValue().set(HotItemController.HOT_ITEM_STOCK_KEY_PREFIX + itemId, String.valueOf(hotItem.getStock()));
        return "success";
    }

    @PostMapping("manage/cancelHotItem")
    public String cancelHotItem(@RequestParam Long itemId) {
        redisTemplate.opsForHash().delete(HotItemController.HOT_ITEM_KEY, String.valueOf(itemId));
        redisTemplate.delete(HotItemController.HOT_ITEM_STOCK_KEY_PREFIX + itemId);
        return "success";
    }

    private void syncHotItemCache(Long itemId) {
        Object hotItemValue = redisTemplate.opsForHash().get(HotItemController.HOT_ITEM_KEY, String.valueOf(itemId));
        if (hotItemValue == null) {
            return;
        }
        Item latestItem = itemService.getById(itemId);
        if (latestItem == null || !ITEM_STATUS_NORMAL.equals(latestItem.getStatus())) {
            redisTemplate.opsForHash().delete(HotItemController.HOT_ITEM_KEY, String.valueOf(itemId));
            redisTemplate.delete(HotItemController.HOT_ITEM_STOCK_KEY_PREFIX + itemId);
            return;
        }
        redisTemplate.opsForHash().put(HotItemController.HOT_ITEM_KEY, String.valueOf(itemId), toHotItemDTO(latestItem));
        redisTemplate.opsForValue().set(HotItemController.HOT_ITEM_STOCK_KEY_PREFIX + itemId, String.valueOf(latestItem.getStock()));
    }

    private ItemDTO toHotItemDTO(Item item) {
        ItemDTO dto = BeanUtils.copyBean(item, ItemDTO.class);
        dto.setHot(Boolean.TRUE);
        return dto;
    }

    private String saveImage(MultipartFile file) throws IOException {
        String projectPath = System.getProperty("user.dir");
        String uploadPath = projectPath + File.separator + "static" + File.separator;

        String originalFilename = file.getOriginalFilename();
        String suffix = ".jpg";
        if (originalFilename != null && originalFilename.lastIndexOf(".") > -1) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID() + suffix;

        File destDir = new File(uploadPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File destFile = new File(uploadPath + newFileName);
        file.transferTo(destFile);

        return "/uploads/" + newFileName;
    }
}
