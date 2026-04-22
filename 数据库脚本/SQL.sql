1 CREATE TABLE item_comment (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
                                user_id BIGINT NOT NULL COMMENT '用户ID',
                                item_id BIGINT NOT NULL COMMENT '项目/商品ID',
                                content VARCHAR(300) NOT NULL COMMENT '评论内容',
                                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 添加索引以提升查询效率
                                INDEX idx_item_id (item_id),
                                INDEX idx_user_id (user_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项评论表';

2
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` bigint NOT NULL AUTO_INCREMENT,
                                      `username` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '用户名',
                                      `password` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '密码，加密存储',
                                      `phone` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '注册手机号',
                                      `create_time` datetime NOT NULL COMMENT '创建时间',
                                      `update_time` datetime NOT NULL,
                                      `status` int DEFAULT '1' COMMENT '使用状态（1正常 2冻结）',
                                      `balance` int DEFAULT NULL COMMENT '账户余额',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      UNIQUE KEY `username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='用户表';
3 CREATE TABLE IF NOT EXISTS `pay_order` (
                                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
                                             `biz_order_no` bigint NOT NULL COMMENT '业务订单号',
                                             `pay_order_no` bigint NOT NULL DEFAULT '0' COMMENT '支付单号',
                                             `biz_user_id` bigint NOT NULL COMMENT '支付用户id',
                                             `pay_channel_code` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '0' COMMENT '支付渠道编码',
                                             `amount` int NOT NULL COMMENT '支付金额，单位分',
                                             `pay_type` tinyint NOT NULL DEFAULT '5' COMMENT '支付类型，1：h5,2:小程序，3：公众号，4：扫码，5：余额支付',
                                             `status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态，0：待提交，1:待支付，2：支付超时或取消，3：支付成功',
                                             `expand_json` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '拓展字段，用于传递不同渠道单独处理的字段',
                                             `result_code` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '第三方返回业务码',
                                             `result_msg` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '第三方返回提示信息',
                                             `pay_success_time` datetime DEFAULT NULL COMMENT '支付成功时间',
                                             `pay_over_time` datetime NOT NULL COMMENT '支付超时时间',
                                             `qr_code_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '支付二维码链接',
                                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `creater` bigint NOT NULL DEFAULT '0' COMMENT '创建人',
                                             `updater` bigint NOT NULL DEFAULT '0' COMMENT '更新人',
                                             `is_delete` bit(1) NOT NULL DEFAULT b'0' COMMENT '逻辑删除',
                                             PRIMARY KEY (`id`) USING BTREE,
                                             UNIQUE KEY `biz_order_no` (`biz_order_no`) USING BTREE,
                                             UNIQUE KEY `pay_order_no` (`pay_order_no`) USING BTREE
  ) ENGINE=InnoDB AUTO_INCREMENT=1659160218174607364 DEFAULT CHARSET=utf8mb3 COMMENT='支付订单';
4 CREATE TABLE IF NOT EXISTS `order_logistics` (
                                                   `order_id` bigint NOT NULL COMMENT '订单id，与订单表一对一',
                                                   `logistics_number` varchar(18) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '物流单号',
                                                   `logistics_company` varchar(18) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '物流公司名称',
                                                   `contact` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '收件人',
                                                   `mobile` varchar(11) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '收件人手机号码',
                                                   `province` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '省',
                                                   `city` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '市',
                                                   `town` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '区',
                                                   `street` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '街道',
                                                   `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                   `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                   PRIMARY KEY (`order_id`) USING BTREE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT;
5 CREATE TABLE IF NOT EXISTS `order_detail` (
                                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单详情id ',
                                                `order_id` bigint NOT NULL COMMENT '订单id',
                                                `item_id` bigint NOT NULL COMMENT 'sku商品id',
                                                `num` int NOT NULL COMMENT '购买数量',
                                                `name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '商品标题',
                                                `spec` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '商品动态属性键值集',
                                                `price` int NOT NULL COMMENT '价格,单位：分',
                                                `image` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '商品图片',
                                                `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                PRIMARY KEY (`id`) USING BTREE,
                                                KEY `key_order_id` (`order_id`) USING BTREE
  ) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='订单详情表';
6 CREATE TABLE IF NOT EXISTS `order` (
                                         `id` bigint NOT NULL COMMENT '订单id',
                                         `total_fee` int NOT NULL DEFAULT '0' COMMENT '总金额，单位为分',
                                         `payment_type` tinyint(1) unsigned zerofill NOT NULL COMMENT '支付类型，1、支付宝，2、微信，3、扣减余额',
                                         `user_id` bigint NOT NULL COMMENT '用户id',
                                         `status` tinyint(1) DEFAULT NULL COMMENT '订单的状态，1、未付款 2、已付款,未发货 3、已发货,未确认 4、确认收货，交易成功 5、交易取消，订单关闭 6、交易结束，已评价',
                                         `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
                                         `consign_time` timestamp NULL DEFAULT NULL COMMENT '发货时间',
                                         `end_time` timestamp NULL DEFAULT NULL COMMENT '交易完成时间',
                                         `close_time` timestamp NULL DEFAULT NULL COMMENT '交易关闭时间',
                                         `comment_time` timestamp NULL DEFAULT NULL COMMENT '评价时间',
                                         `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         PRIMARY KEY (`id`) USING BTREE,
                                         KEY `multi_key_status_time` (`status`,`create_time`) USING BTREE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8_bin ROW_FORMAT=COMPACT;
7 CREATE TABLE IF NOT EXISTS `item` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品id',
                                        `name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'SKU名称',
                                        `price` int NOT NULL DEFAULT '0' COMMENT '价格（分）',
                                        `stock` int UNSIGNED NOT NULL COMMENT '库存数量',
                                        `image` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '商品图片',
                                        `category` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '类目名称',
                                        `brand` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '品牌名称',
                                        `spec` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '规格',
                                        `sold` int DEFAULT '0' COMMENT '销量',
                                        `comment_count` int DEFAULT '0' COMMENT '评论数',
                                        `isAD` tinyint(1) DEFAULT '0' COMMENT '是否是推广广告，true/false',
                                        `status` int DEFAULT '2' COMMENT '商品状态 1-正常，2-下架，3-删除',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        `creater` bigint DEFAULT NULL COMMENT '创建人',
                                        `updater` bigint DEFAULT NULL COMMENT '修改人',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        KEY `status` (`status`) USING BTREE,
                                        KEY `updated` (`update_time`) USING BTREE,
                                        KEY `category` (`category`)
  ) ENGINE=InnoDB AUTO_INCREMENT=100002672305 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='商品表';
8 CREATE TABLE IF NOT EXISTS `address` (
                                           `id` bigint NOT NULL AUTO_INCREMENT,
                                           `user_id` bigint DEFAULT NULL COMMENT '用户ID',
                                           `province` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '省',
                                           `city` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '市',
                                           `town` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '县/区',
                                           `mobile` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '手机',
                                           `street` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '详细地址',
                                           `contact` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '联系人',
                                           `is_default` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '是否是默认 1默认 0否',
                                           `notes` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '备注',
                                           PRIMARY KEY (`id`) USING BTREE,
                                           KEY `user_id` (`user_id`) USING BTREE
  ) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT;
9 CREATE TABLE IF NOT EXISTS `cart` (
                                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '购物车条目id ',
                                        `user_id` bigint NOT NULL COMMENT '用户id',
                                        `item_id` bigint NOT NULL COMMENT 'sku商品id',
                                        `num` int NOT NULL DEFAULT '1' COMMENT '购买数量',
                                        `name` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '商品标题',
                                        `spec` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '商品动态属性键值集',
                                        `price` int NOT NULL COMMENT '价格,单位：分',
                                        `image` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' COMMENT '商品图片',
                                        `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                        `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        PRIMARY KEY (`id`) USING BTREE,
                                        KEY `key_user_item_id` (`user_id`,`item_id`) USING BTREE
  ) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb3 ROW_FORMAT=COMPACT COMMENT='订单详情表';


10 浏览历史表
CREATE TABLE IF NOT EXISTS `browse_history` (
                                                `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '浏览历史ID',
                                                `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                                `item_id` bigint(20) NOT NULL COMMENT '商品ID',
                                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                PRIMARY KEY (`id`),
                                                KEY `idx_user_id` (`user_id`),
                                                KEY `idx_item_id` (`item_id`),
                                                KEY `idx_user_item` (`user_id`, `item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户浏览历史表';
