package com.hmall.pay.corn;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmall.common.utils.UserContext;
import com.hmall.pay.domain.po.PayOrder;
import com.hmall.pay.service.IPayOrderService;
import com.hmall.pay.service.impl.PayOrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ClassName: PayCorn
 * Package: com.hmall.pay.corn
 *
 * @Author liang
 * @Create 2026/4/6 22:09
 * Description:
 */
@Component
@RequiredArgsConstructor
public class PayCorn {
    /**
     * Cron 表达式就像是一把“定时闹钟”的钥匙。在 Spring Task、Linux (crontab) 或者 Quartz 中，它被广泛用于定义任务触发的时间规律。
     * 虽然不同框架的 Cron 表达式略有差异（比如 Spring 支持 6 位，Linux 只支持 5 位），但核心逻辑是一致的。
     * 1. 结构拆解
     * Spring 中的 Cron 表达式通常由 6 个空格隔开的域组成：
     *
     * 顺序	字段	允许值	允许的特殊字符
     * 1	秒	0-59	, - * /
     * 2	分	0-59	, - * /
     * 3	时	0-23	, - * /
     * 4	日	1-31	, - * / ? L W
     * 5	月	1-12 或 JAN-DEC	, - * /
     * 6	周	1-7 或 SUN-SAT	, - * / ? L #
     * 2. 特殊符号的含义（这是理解的关键）
     * * (所有值)：代表“每”。在“秒”的位置就是每秒，在“月”的位置就是每月。
     *
     * ? (不指定值)：非常重要！ 仅用于“日”和“周”。因为“日”和“周”可能冲突。比如你指定了每月 15 号，就不方便指定它是周几，这时周的位置就写 ?。
     *
     * - (范围)：代表“从...到...”。例如在“时”写 9-11，表示 9点、10点、11点各触发一次。
     *
     * , (枚举)：代表“和”。例如在“周”写 MON,WED,FRI，表示周一、周三、周五执行。
     *
     * / (增量)：代表“每隔”。例如在“分”写 0/15，表示从 0 分开始，每隔 15 分钟执行一次（0, 15, 30, 45）。
     *
     * L (Last)：仅用于“日”和“周”，表示最后一天。在“日”里表示月底（28/30/31号），在“周”里 6L 表示该月最后一个周五。
     */
    private  final IPayOrderService payOrderService;
    @Scheduled(cron = "0 0/30 * * * ?")//后面调成每5分钟
    //mp默认不更新null值
    public void checkPayStatus(){

        LambdaQueryWrapper<PayOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PayOrder::getStatus,1);
        //全部订单，未支付
        List<PayOrder> list = payOrderService.list(queryWrapper);
        for (PayOrder payOrder : list) {
            LocalDateTime createTime = payOrder.getCreateTime();
            LocalDateTime now = LocalDateTime.now();
            if(now.isAfter(createTime.plusMinutes(30L))){
                //已经超时了
                payOrder.setStatus(2);
                payOrderService.saveOrUpdate(payOrder);
            }


        }
    }

}
