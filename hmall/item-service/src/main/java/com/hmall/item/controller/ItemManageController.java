package com.hmall.item.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.dto.ItemDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("manage/add")
    public String addItem(ItemDTO itemdto, @RequestParam MultipartFile file) throws IOException {
        String projectPath = System.getProperty("user.dir");
        String uploadPath = projectPath + File.separator + "static" + File.separator;
        // 1. 检查文件是否为空
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        // 2. 获取原文件名并生成唯一文件名（防止重名覆盖）
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + suffix;

        // 3. 创建保存目录
        File destDir = new File(uploadPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        // 4. 保存文件到本地磁盘
        File destFile = new File(uploadPath + newFileName);
        file.transferTo(destFile);//保存图片到磁盘上
        //5 数据库中存储图片路径，以 /uploads/ 开头方便前端代理访问
        String image = "/uploads/" + newFileName;
        Item item = new Item();
        BeanUtils.copyProperties(itemdto, item);
        item.setImage(image);
        itemService.save( item);
        return "success";
    }

    @PutMapping("manage/update")
    public String updateItem(@RequestBody ItemDTO itemDTO) {
        Item item = new Item();
        BeanUtils.copyProperties(itemDTO, item);
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

}
