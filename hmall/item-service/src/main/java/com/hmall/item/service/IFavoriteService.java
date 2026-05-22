package com.hmall.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.api.dto.ItemDTO;
import com.hmall.item.domain.po.Favorite;

import java.util.List;

public interface IFavoriteService extends IService<Favorite> {

    void addFavorite(Long itemId);

    void removeFavorite(Long itemId);

    List<ItemDTO> getMyFavorites();

    boolean isFavorited(Long itemId);
}
