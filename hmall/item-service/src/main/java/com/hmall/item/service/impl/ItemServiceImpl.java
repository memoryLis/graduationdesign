package com.hmall.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.TradeClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.dto.UserOrderInteractionDTO;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.item.domain.po.BrowseHistory;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemComment;
import com.hmall.item.mapper.BrowseHistoryMapper;
import com.hmall.item.mapper.ItemCommentMapper;
import com.hmall.item.mapper.ItemMapper;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private static final int RECOMMEND_SIZE = 10;
    private static final int ORDER_WEIGHT = 3;
    private static final int COMMENT_WEIGHT = 2;
    private static final int BROWSE_WEIGHT = 1;
    private static final int CATEGORY_REPEAT_LIMIT = 5;
    private static final int FALLBACK_LIMIT = 30;

    private static final double CF_ORDER_WEIGHT = 4D;
    private static final double CF_BROWSE_WEIGHT = 1D;
    private static final double CF_COMMENT_WEIGHT = 3D;

    private final TradeClient tradeClient;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final ItemCommentMapper itemCommentMapper;

    @Override
    public void deductStock(List<OrderDetailDTO> items) {
        String sqlStatement = "com.hmall.item.mapper.ItemMapper.updateStock";
        boolean result;
        try {
            result = executeBatch(items, (sqlSession, entity) -> sqlSession.update(sqlStatement, entity));
        } catch (Exception e) {
            throw new BizIllegalException("更新库存异常，可能是库存不足", e);
        }
        if (!result) {
            throw new BizIllegalException("库存不足");
        }
    }

    @Override
    public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
        return BeanUtils.copyList(listByIds(ids), ItemDTO.class);
    }

    @Override
    public List<ItemDTO> getUserLike() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return BeanUtils.copyList(queryFallbackItems(Collections.emptySet(), RECOMMEND_SIZE), ItemDTO.class);
        }

        Set<Long> interactedItemIds = new HashSet<>();
        Map<String, Integer> categoryWeights = new HashMap<>();

        mergeOrderCategories(interactedItemIds, categoryWeights);
        mergeBrowseCategories(userId, interactedItemIds, categoryWeights);
        mergeCommentCategories(userId, interactedItemIds, categoryWeights);

        if (categoryWeights.isEmpty()) {
            return BeanUtils.copyList(queryFallbackItems(interactedItemIds, RECOMMEND_SIZE), ItemDTO.class);
        }

        List<String> preferredCategories = buildWeightedCategories(categoryWeights);
        List<Item> candidates = queryCategoryItems(preferredCategories, interactedItemIds);
        List<Item> recommended = pickRandomItemsByCategory(candidates, preferredCategories, RECOMMEND_SIZE);

        if (recommended.size() < RECOMMEND_SIZE) {
            Set<Long> excludeIds = recommended.stream().map(Item::getId).collect(Collectors.toSet());
            excludeIds.addAll(interactedItemIds);
            recommended.addAll(queryFallbackItems(excludeIds, RECOMMEND_SIZE - recommended.size()));
        }

        return BeanUtils.copyList(recommended, ItemDTO.class);
    }

    @Override
    public List<ItemDTO> searchFavorite() {
        Long userId = UserContext.getUser();
        List<Item> activeItems = lambdaQuery()
                .eq(Item::getStatus, 1)
                .list();
        if (CollUtils.isEmpty(activeItems)) {
            return Collections.emptyList();
        }

        Map<Long, Item> itemMap = activeItems.stream()
                .collect(Collectors.toMap(Item::getId, item -> item, (a, b) -> a));
        Map<Long, Map<Long, Double>> userItemScores = new HashMap<>();
        Map<Long, Map<String, Double>> userCategoryScores = new HashMap<>();

        mergeOrderInteractions(userItemScores, userCategoryScores, itemMap);
        mergeBrowseInteractions(userItemScores, userCategoryScores, itemMap);
        mergeCommentInteractions(userItemScores, userCategoryScores, itemMap);

        if (userId == null) {
            return BeanUtils.copyList(fillFallbackItems(itemMap, Collections.emptySet(), Collections.emptyMap(), RECOMMEND_SIZE), ItemDTO.class);
        }

        Map<Long, Double> targetItemScores = userItemScores.getOrDefault(userId, Collections.emptyMap());
        Map<String, Double> targetCategoryScores = userCategoryScores.getOrDefault(userId, Collections.emptyMap());
        Set<Long> interactedItemIds = new HashSet<>(targetItemScores.keySet());

        if (targetItemScores.isEmpty() && targetCategoryScores.isEmpty()) {
            return BeanUtils.copyList(fillFallbackItems(itemMap, Collections.emptySet(), Collections.emptyMap(), RECOMMEND_SIZE), ItemDTO.class);
        }

        Map<Long, Double> collaborativeScores = new HashMap<>();
        for (Map.Entry<Long, Map<String, Double>> entry : userCategoryScores.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId == null || otherUserId.equals(userId)) {
                continue;
            }
            double similarity = cosineSimilarity(targetCategoryScores, entry.getValue());
            if (similarity <= 0D) {
                continue;
            }
            Map<Long, Double> otherItemScores = userItemScores.getOrDefault(otherUserId, Collections.emptyMap());
            for (Map.Entry<Long, Double> itemEntry : otherItemScores.entrySet()) {
                Long itemId = itemEntry.getKey();
                if (itemId == null || interactedItemIds.contains(itemId) || !itemMap.containsKey(itemId)) {
                    continue;
                }
                collaborativeScores.merge(itemId, similarity * itemEntry.getValue(), Double::sum);
            }
        }

        List<Item> recommended = collaborativeScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        finalScore(b.getKey(), b.getValue(), targetCategoryScores, itemMap),
                        finalScore(a.getKey(), a.getValue(), targetCategoryScores, itemMap)))
                .map(entry -> itemMap.get(entry.getKey()))
                .filter(Objects::nonNull)
                .limit(RECOMMEND_SIZE)
                .collect(Collectors.toList());

        if (recommended.size() < RECOMMEND_SIZE) {
            Set<Long> excludeIds = recommended.stream().map(Item::getId).collect(Collectors.toSet());
            excludeIds.addAll(interactedItemIds);
            recommended.addAll(fillFallbackItems(itemMap, excludeIds, targetCategoryScores, RECOMMEND_SIZE - recommended.size()));
        }

        return BeanUtils.copyList(recommended, ItemDTO.class);
    }

    private void mergeOrderCategories(Set<Long> interactedItemIds, Map<String, Integer> categoryWeights) {
        List<UserOrderInteractionDTO> interactions;
        try {
            interactions = tradeClient.queryMyOrderInteractions();
        } catch (Exception e) {
            interactions = Collections.emptyList();
        }
        if (CollUtils.isEmpty(interactions)) {
            return;
        }

        List<Long> itemIds = interactions.stream()
                .map(UserOrderInteractionDTO::getItemId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(itemIds)) {
            return;
        }

        Map<Long, Item> itemMap = listByIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, item -> item, (a, b) -> a));

        for (UserOrderInteractionDTO interaction : interactions) {
            Item item = itemMap.get(interaction.getItemId());
            if (item == null) {
                continue;
            }
            interactedItemIds.add(item.getId());
            addCategoryWeight(categoryWeights, item.getCategory(), ORDER_WEIGHT);
        }
    }

    private void mergeBrowseCategories(Long userId, Set<Long> interactedItemIds, Map<String, Integer> categoryWeights) {
        List<BrowseHistory> histories = browseHistoryMapper.selectList(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .orderByDesc(BrowseHistory::getUpdateTime)
                .last("LIMIT 50"));
        if (CollUtils.isEmpty(histories)) {
            return;
        }

        List<Long> itemIds = histories.stream()
                .map(BrowseHistory::getItemId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Item> itemMap = listByIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, item -> item, (a, b) -> a));

        for (BrowseHistory history : histories) {
            Item item = itemMap.get(history.getItemId());
            if (item == null) {
                continue;
            }
            interactedItemIds.add(item.getId());
            addCategoryWeight(categoryWeights, item.getCategory(), BROWSE_WEIGHT);
        }
    }

    private void mergeCommentCategories(Long userId, Set<Long> interactedItemIds, Map<String, Integer> categoryWeights) {
        List<ItemComment> comments = itemCommentMapper.selectList(new LambdaQueryWrapper<ItemComment>()
                .eq(ItemComment::getUserId, userId)
                .orderByDesc(ItemComment::getCreateTime)
                .last("LIMIT 50"));
        if (CollUtils.isEmpty(comments)) {
            return;
        }

        List<Long> itemIds = comments.stream()
                .map(ItemComment::getItemId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Item> itemMap = listByIds(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, item -> item, (a, b) -> a));

        for (ItemComment comment : comments) {
            Item item = itemMap.get(comment.getItemId());
            if (item == null) {
                continue;
            }
            interactedItemIds.add(item.getId());
            addCategoryWeight(categoryWeights, item.getCategory(), COMMENT_WEIGHT);
        }
    }

    private void addCategoryWeight(Map<String, Integer> categoryWeights, String category, int weight) {
        if (category == null || category.trim().isEmpty()) {
            return;
        }
        categoryWeights.merge(category, weight, Integer::sum);
    }

    private List<String> buildWeightedCategories(Map<String, Integer> categoryWeights) {
        List<Map.Entry<String, Integer>> sortedCategories = categoryWeights.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        List<String> weightedCategories = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sortedCategories) {
            int repeat = Math.min(entry.getValue(), CATEGORY_REPEAT_LIMIT);
            for (int i = 0; i < repeat; i++) {
                weightedCategories.add(entry.getKey());
            }
        }
        Collections.shuffle(weightedCategories);
        return weightedCategories;
    }

    private List<Item> queryCategoryItems(List<String> preferredCategories, Set<Long> interactedItemIds) {
        if (CollUtils.isEmpty(preferredCategories)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, 1)
                .in(Item::getCategory, preferredCategories);
        if (CollUtils.isNotEmpty(interactedItemIds)) {
            wrapper.notIn(Item::getId, interactedItemIds);
        }
        return list(wrapper);
    }

    private List<Item> pickRandomItemsByCategory(List<Item> candidates, List<String> preferredCategories, int limit) {
        if (CollUtils.isEmpty(candidates) || CollUtils.isEmpty(preferredCategories) || limit <= 0) {
            return new ArrayList<>();
        }

        Map<String, List<Item>> categoryItemMap = candidates.stream()
                .collect(Collectors.groupingBy(Item::getCategory, LinkedHashMap::new, Collectors.toList()));
        categoryItemMap.values().forEach(Collections::shuffle);

        List<Item> result = new ArrayList<>(limit);
        Set<Long> selectedIds = new HashSet<>();
        int index = 0;
        int maxRounds = preferredCategories.size() * 2;

        while (result.size() < limit && index < maxRounds) {
            for (String category : preferredCategories) {
                List<Item> items = categoryItemMap.get(category);
                if (CollUtils.isEmpty(items)) {
                    continue;
                }
                Item item = items.remove(0);
                if (selectedIds.add(item.getId())) {
                    result.add(item);
                }
                if (result.size() >= limit) {
                    break;
                }
            }
            index++;
        }

        if (result.size() < limit) {
            List<Item> remaining = candidates.stream()
                    .filter(item -> !selectedIds.contains(item.getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(remaining);
            for (Item item : remaining) {
                result.add(item);
                if (result.size() >= limit) {
                    break;
                }
            }
        }

        return result;
    }

    private List<Item> queryFallbackItems(Set<Long> excludeIds, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, 1)
                .orderByDesc(Item::getSold)
                .orderByDesc(Item::getCommentCount)
                .last("LIMIT " + FALLBACK_LIMIT);
        if (CollUtils.isNotEmpty(excludeIds)) {
            wrapper.notIn(Item::getId, excludeIds);
        }

        List<Item> fallbackItems = list(wrapper);
        if (CollUtils.isEmpty(fallbackItems)) {
            return Collections.emptyList();
        }
        Collections.shuffle(fallbackItems, ThreadLocalRandom.current());
        return fallbackItems.stream().limit(limit).collect(Collectors.toList());
    }

    private void mergeOrderInteractions(Map<Long, Map<Long, Double>> userItemScores,
                                        Map<Long, Map<String, Double>> userCategoryScores,
                                        Map<Long, Item> itemMap) {
        List<UserOrderInteractionDTO> interactions;
        try {
            interactions = tradeClient.queryOrderInteractions();
        } catch (Exception e) {
            interactions = Collections.emptyList();
        }
        if (CollUtils.isEmpty(interactions)) {
            return;
        }

        for (UserOrderInteractionDTO interaction : interactions) {
            if (interaction == null || interaction.getUserId() == null || interaction.getItemId() == null) {
                continue;
            }
            Item item = itemMap.get(interaction.getItemId());
            if (item == null) {
                continue;
            }
            int count = interaction.getNum() == null || interaction.getNum() < 1 ? 1 : interaction.getNum();
            addInteraction(userItemScores, userCategoryScores, interaction.getUserId(), item, CF_ORDER_WEIGHT * count);
        }
    }

    private void mergeBrowseInteractions(Map<Long, Map<Long, Double>> userItemScores,
                                         Map<Long, Map<String, Double>> userCategoryScores,
                                         Map<Long, Item> itemMap) {
        List<BrowseHistory> histories = browseHistoryMapper.selectList(null);
        if (CollUtils.isEmpty(histories)) {
            return;
        }

        for (BrowseHistory history : histories) {
            if (history == null || history.getUserId() == null || history.getItemId() == null) {
                continue;
            }
            Item item = itemMap.get(history.getItemId());
            if (item == null) {
                continue;
            }
            addInteraction(userItemScores, userCategoryScores, history.getUserId(), item, CF_BROWSE_WEIGHT);
        }
    }

    private void mergeCommentInteractions(Map<Long, Map<Long, Double>> userItemScores,
                                          Map<Long, Map<String, Double>> userCategoryScores,
                                          Map<Long, Item> itemMap) {
        List<ItemComment> comments = itemCommentMapper.selectList(null);
        if (CollUtils.isEmpty(comments)) {
            return;
        }

        for (ItemComment comment : comments) {
            if (comment == null || comment.getUserId() == null || comment.getItemId() == null) {
                continue;
            }
            Item item = itemMap.get(comment.getItemId());
            if (item == null) {
                continue;
            }
            addInteraction(userItemScores, userCategoryScores, comment.getUserId(), item, CF_COMMENT_WEIGHT);
        }
    }

    private void addInteraction(Map<Long, Map<Long, Double>> userItemScores,
                                Map<Long, Map<String, Double>> userCategoryScores,
                                Long userId,
                                Item item,
                                double weight) {
        userItemScores.computeIfAbsent(userId, key -> new HashMap<>())
                .merge(item.getId(), weight, Double::sum);

        String category = item.getCategory();
        if (category != null && !category.trim().isEmpty()) {
            userCategoryScores.computeIfAbsent(userId, key -> new HashMap<>())
                    .merge(category, weight, Double::sum);
        }
    }

    private double cosineSimilarity(Map<String, Double> target, Map<String, Double> candidate) {
        if (target.isEmpty() || candidate.isEmpty()) {
            return 0D;
        }

        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (Map.Entry<String, Double> entry : target.entrySet()) {
            double left = entry.getValue() == null ? 0D : entry.getValue();
            leftNorm += left * left;
            dot += left * candidate.getOrDefault(entry.getKey(), 0D);
        }
        for (Double value : candidate.values()) {
            double right = value == null ? 0D : value;
            rightNorm += right * right;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double finalScore(Long itemId,
                              Double collaborativeScore,
                              Map<String, Double> targetCategoryScores,
                              Map<Long, Item> itemMap) {
        Item item = itemMap.get(itemId);
        if (item == null) {
            return Double.NEGATIVE_INFINITY;
        }
        double categoryScore = targetCategoryScores.getOrDefault(item.getCategory(), 0D);
        double soldScore = item.getSold() == null ? 0D : item.getSold();
        double commentScore = item.getCommentCount() == null ? 0D : item.getCommentCount();
        return (collaborativeScore == null ? 0D : collaborativeScore)
                + categoryScore * 0.35D
                + soldScore * 0.08D
                + commentScore * 0.12D;
    }

    private List<Item> fillFallbackItems(Map<Long, Item> itemMap,
                                         Set<Long> excludeIds,
                                         Map<String, Double> targetCategoryScores,
                                         int size) {
        if (size <= 0) {
            return Collections.emptyList();
        }
        Set<Long> safeExcludeIds = excludeIds == null ? Collections.emptySet() : excludeIds;
        Map<String, Double> safeCategoryScores = targetCategoryScores == null ? Collections.emptyMap() : targetCategoryScores;

        return itemMap.values().stream()
                .filter(item -> !safeExcludeIds.contains(item.getId()))
                .sorted(Comparator.comparingDouble((Item item) ->
                        safeCategoryScores.getOrDefault(item.getCategory(), 0D) * 10D
                                + (item.getSold() == null ? 0D : item.getSold())
                                + (item.getCommentCount() == null ? 0D : item.getCommentCount()) * 2D).reversed())
                .limit(size)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
