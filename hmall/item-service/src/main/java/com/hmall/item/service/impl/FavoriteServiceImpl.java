package com.hmall.item.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.hmall.item.domain.po.Favorite;
import com.hmall.item.mapper.FavoriteMapper;
import com.hmall.item.service.IFavoriteService;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements IFavoriteService {

    private final IItemService itemService;

    @Override
    public void addFavorite(Long itemId) {
        Long userId = UserContext.getUser();
        long count = lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId)
                .count();
        if (count > 0) {
            throw new BadRequestException("已收藏过该商品");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        save(favorite);
    }

    @Override
    public void removeFavorite(Long itemId) {
        Long userId = UserContext.getUser();
        lambdaUpdate()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId)
                .remove();
    }

    @Override
    public List<ItemDTO> getMyFavorites() {
        Long userId = UserContext.getUser();
        List<Favorite> favorites = lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime)
                .list();

        List<Long> itemIds = favorites.stream()
                .map(Favorite::getItemId)
                .collect(Collectors.toList());
        if (itemIds.isEmpty()) {
            return List.of();
        }
        return itemService.queryItemByIds(itemIds);
    }

    @Override
    public boolean isFavorited(Long itemId) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return false;
        }
        return lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId)
                .count() > 0;
    }
}
