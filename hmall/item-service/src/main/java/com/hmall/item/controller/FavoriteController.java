package com.hmall.item.controller;

import com.hmall.api.dto.ItemDTO;
import com.hmall.common.utils.UserContext;
import com.hmall.item.service.IFavoriteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "商品收藏管理接口")
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final IFavoriteService favoriteService;

    @ApiOperation("收藏商品")
    @PostMapping("/{itemId}")
    public void addFavorite(@ApiParam("商品id") @PathVariable("itemId") Long itemId) {
        favoriteService.addFavorite(itemId);
    }

    @ApiOperation("取消收藏")
    @DeleteMapping("/{itemId}")
    public void removeFavorite(@ApiParam("商品id") @PathVariable("itemId") Long itemId) {
        favoriteService.removeFavorite(itemId);
    }

    @ApiOperation("获取我的收藏列表")
    @GetMapping
    public List<ItemDTO> getMyFavorites() {
        return favoriteService.getMyFavorites();
    }

    @ApiOperation("检查商品是否已收藏")
    @GetMapping("/check/{itemId}")
    public Map<String, Boolean> isFavorited(@PathVariable("itemId") Long itemId) {
        return Map.of("favorited", favoriteService.isFavorited(itemId));
    }
}
