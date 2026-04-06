package com.hmall.item;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ClassName: ItemConstant
 * Package: com.hmall.item
 * Description:
 *
 * @Author liang
 * @Create 2026/3/25 21:46
 * @Version jdk17.0
 */
public class ItemConstant {
    // 原有种类
    public static final String CATEGORY_LUGGAGE = "拉杆箱";
    public static final String CATEGORY_MILK = "牛奶";
    public static final String CATEGORY_PHONE = "手机";
    public static final String CATEGORY_HARD_DRIVE = "硬盘";

    // 新增分类
    public static final String CATEGORY_FRUIT = "水果";
    public static final String CATEGORY_TOY = "玩具";
    public static final String CATEGORY_COSMETICS = "美妆";
    public static final String CATEGORY_BOOK = "图书";
    public static final String CATEGORY_SNACK = "零食";
    public static final String CATEGORY_OUTDOOR = "户外运动";

    public static final String[] CATEGORYS = {"拉杆箱", "牛奶", "手机", "硬盘", "水果", "玩具", "美妆", "图书", "零食", "户外运动"};

    public static final List<String> CATEGORY_LIST = Collections.unmodifiableList(Arrays.asList(CATEGORYS));
}
