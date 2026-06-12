package com.hmall.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.TradeClient;
import com.hmall.api.client.UserClient;
import com.hmall.api.vo.OrderPayDetailVO;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.pay.domain.dto.PayApplyDTO;
import com.hmall.pay.domain.dto.PayOrderFormDTO;
import com.hmall.pay.domain.po.PayOrder;
import com.hmall.pay.service.IPayOrderService;
import com.hmall.pay.enums.PayStatus;
import com.hmall.pay.mapper.PayOrderMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 支付订单 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-16
 */
@Service
@RequiredArgsConstructor
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements IPayOrderService {
    private final TradeClient tradeClient;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;
    private final RedissonClient redissonClient;
    @Override
    public String applyPayOrder(PayApplyDTO applyDTO) {
        // 1.幂等性校验
        PayOrder payOrder = checkIdempotent(applyDTO);
        // 2.返回结果
        return payOrder.getId().toString();
    }

    @Override
    @Transactional
    public void tryPayOrderByBalance(PayOrderFormDTO payOrderFormDTO) {
        // 1.查询支付单
        PayOrder po = getById(payOrderFormDTO.getId());
        // 2.判断状态
        if(!PayStatus.WAIT_BUYER_PAY.equalsValue(po.getStatus())){
            throw new BizIllegalException("交易已支付或关闭！");
        }
        // 3.分布式锁防重：对同一笔支付单加锁，防止并发重复支付
        String lockKey = "pay_order:" + payOrderFormDTO.getId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                throw new BizIllegalException("支付处理中，请稍后再试！");
            }
            // 4.获取锁后重新查询状态（双重检查）
            po = getById(payOrderFormDTO.getId());
            if(!PayStatus.WAIT_BUYER_PAY.equalsValue(po.getStatus())){
                throw new BizIllegalException("交易已支付或关闭！");
            }
            // 5.尝试扣减余额
            userClient.deductMoney(payOrderFormDTO.getPw(), po.getAmount());
            // 6.修改支付单状态
            boolean success = markPayOrderSuccess(payOrderFormDTO.getId(), LocalDateTime.now());
            if (!success) {
                throw new BizIllegalException("交易已支付或关闭！");
            }
            // 7.修改订单状态
            rabbitTemplate.convertAndSend("pay.direct", "pay.success", po.getBizOrderNo());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizIllegalException("支付处理中断！");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 根据id查找订单详情
     * @param id
     * @return
     */
    @Override
    public OrderPayDetailVO getPayOrderDetail(Long id) {
        PayOrder payOrder = getById(id);
        if(payOrder == null){
            throw new RuntimeException("支付单不存在！");
        }
        if (!UserContext.getUser().equals(payOrder.getBizUserId()) && !UserContext.isAdmin()) {
            throw new BizIllegalException("无权查看该支付单");
        }
        //拿到orderId
        Long orderId = payOrder.getBizOrderNo();
        return tradeClient.getPayOrderDetail(orderId);


    }

    public boolean markPayOrderSuccess(Long id, LocalDateTime successTime) {
        return lambdaUpdate()
                .set(PayOrder::getStatus, PayStatus.TRADE_SUCCESS.getValue())
                .set(PayOrder::getPaySuccessTime, successTime)
                .set(PayOrder::getUpdateTime, successTime)
                .eq(PayOrder::getId, id)
                // 支付状态的乐观锁判断
                .in(PayOrder::getStatus, PayStatus.NOT_COMMIT.getValue(), PayStatus.WAIT_BUYER_PAY.getValue())
                .update();
    }


    private PayOrder checkIdempotent(PayApplyDTO applyDTO) {
        // 1.首先查询支付单
        PayOrder oldOrder = queryByBizOrderNo(applyDTO.getBizOrderNo());
        // 2.判断是否存在
        if (oldOrder == null) {
            // 不存在支付单，说明是第一次，写入新的支付单并返回
            PayOrder payOrder = buildPayOrder(applyDTO);
            payOrder.setPayOrderNo(IdWorker.getId());//支付流水号，mp雪花算法生成。
            save(payOrder);
            return payOrder;
        }
        // 3.旧单已经存在，判断是否支付成功
        if (PayStatus.TRADE_SUCCESS.equalsValue(oldOrder.getStatus())) {
            // 已经支付成功，抛出异常
            throw new BizIllegalException("订单已经支付！");
        }
        // 4.旧单已经存在，判断是否已经关闭
        if (PayStatus.TRADE_CLOSED.equalsValue(oldOrder.getStatus())) {
            // 已经关闭，抛出异常
            throw new BizIllegalException("订单已关闭");
        }
        // 5.旧单已经存在，判断支付渠道是否一致
        if (!StringUtils.equals(oldOrder.getPayChannelCode(), applyDTO.getPayChannelCode())) {
            // 支付渠道不一致，需要重置数据，然后重新申请支付单
            PayOrder payOrder = buildPayOrder(applyDTO);
            payOrder.setId(oldOrder.getId());
            payOrder.setQrCodeUrl("");
            payOrder.setCreateTime(oldOrder.getCreateTime());
            payOrder.setUpdateTime(LocalDateTime.now());
            updateById(payOrder);
            payOrder.setPayOrderNo(oldOrder.getPayOrderNo());
            return payOrder;
        }
        // 6.旧单已经存在，且可能是未支付或未提交，且支付渠道一致，直接返回旧数据
        return oldOrder;
    }

    private PayOrder buildPayOrder(PayApplyDTO payApplyDTO) {
        // 1.数据转换
        PayOrder payOrder = BeanUtils.toBean(payApplyDTO, PayOrder.class);
        LocalDateTime now = LocalDateTime.now();
        // 2.初始化数据
        payOrder.setPayOverTime(now.plusMinutes(30L));
        payOrder.setStatus(PayStatus.WAIT_BUYER_PAY.getValue());
        payOrder.setBizUserId(UserContext.getUser());
        payOrder.setCreateTime(now);
        payOrder.setUpdateTime(now);
        return payOrder;
    }
    public PayOrder queryByBizOrderNo(Long bizOrderNo) {
        return lambdaQuery()
                .eq(PayOrder::getBizOrderNo, bizOrderNo)
                .one();
    }
}
