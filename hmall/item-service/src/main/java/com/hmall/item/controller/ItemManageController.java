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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * ClassName: ItemPictureController
 * Package: com.hmall.item.controller
 *
 * @Author liang
 * @Create 2026/4/6 10:04
 * Description:管理员为账号密码都为123
 * 这个接口用于给管理员添加商品，管理商品
 */
@RestController
@RequestMapping("items")
public class ItemManageController {
    @Autowired
    private IItemService itemService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("manage/add")
    public String addItem(ItemDTO itemdto, @RequestParam MultipartFile file) throws IOException {
        // 1. 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        // 2. 保存图片并回填图片路径
        String image = saveImage(file);
        Item item = new Item();
        BeanUtils.copyProperties(itemdto, item);
        item.setImage(image);
        itemService.save( item);
        return "success";
    }

    @PutMapping("manage/update")
    public String updateItem(ItemDTO itemDTO, @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (itemDTO.getId() == null) {
            throw new RuntimeException("商品id不能为空");
        }
        Item item = new Item();
        BeanUtils.copyProperties(itemDTO, item);

        // 编辑时如果上传了新图片，则替换图片地址；否则保持原图
        if (file != null && !file.isEmpty()) {
            item.setImage(saveImage(file));
        }

        itemService.updateById(item);
        return "success";
    }

    @DeleteMapping("manage/delete/{id}")
    public String deleteItem(@PathVariable Long id) {
        itemService.removeById(id);
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
        Page<Item> page = itemService.page(new Page<>(pageNo, pageSize), wrapper);
        return PageDTO.of(page, ItemDTO.class);
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
    /**
     * 加入为热门商品
     * 首页展示热门商品。热门商品扣减库存通过redis实现。扣减余额和购物车商品通过rabbitmq发送消息实现。
     */
    @PostMapping("manage/addHotItem")
    public String addHotItem(Long itemId) {
        //把热门商品放入redis中
        Item hotItem = itemService.getById(itemId);
        if(!hotItem.getStatus().equals("1")){
            throw new RuntimeException("商品未上架");
        }
        redisTemplate.opsForSet().add("hot_items", hotItem);
        return "success";
    }

}
