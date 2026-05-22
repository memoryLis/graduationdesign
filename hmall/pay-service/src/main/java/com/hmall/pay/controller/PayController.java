package com.hmall.pay.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.api.vo.OrderPayDetailVO;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.UserContext;
import com.hmall.pay.domain.dto.PayApplyDTO;
import com.hmall.pay.domain.dto.PayOrderFormDTO;
import com.hmall.pay.domain.po.PayOrder;
import com.hmall.pay.domain.vo.PayOrderVO;
import com.hmall.pay.enums.PayType;
import com.hmall.pay.service.IPayOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "支付相关接口")
@RestController
@RequestMapping("pay-orders")
@RequiredArgsConstructor
public class PayController {

    private final IPayOrderService payOrderService;

    @ApiOperation("查询支付单")
    @GetMapping
    public PageDTO<PayOrderVO> queryPayOrders( @RequestParam( defaultValue = "1") Long current, @RequestParam(defaultValue = "10") Long size){
        Long userId = UserContext.getUser();
        Page<PayOrder> page = payOrderService.lambdaQuery()
                .eq(PayOrder::getBizUserId, userId)
                .orderByDesc(PayOrder::getCreateTime)
                .page(new Page<>(current, size));
        return PageDTO.of(page, PayOrderVO.class);
    }

    /**
     * 根据支付单ID查询到对应的支付详情
     * @return
     */
    @GetMapping("/getPayOrderDetail")
    public OrderPayDetailVO getPayOrderDetail(@RequestParam Long id){
        //id是支付订单号
        return payOrderService.getPayOrderDetail(id);
    }
    @ApiOperation("生成支付单")
    @PostMapping
    public String applyPayOrder(@RequestBody PayApplyDTO applyDTO){
        if(!PayType.BALANCE.equalsValue(applyDTO.getPayType())){
            // 目前只支持余额支付
            throw new BizIllegalException("抱歉，目前只支持余额支付");
        }
        return payOrderService.applyPayOrder(applyDTO);
    }

    @ApiOperation("尝试基于用户余额支付")
    @ApiImplicitParam(value = "支付单id", name = "id")
    @PostMapping("{id}")
    public void tryPayOrderByBalance(@PathVariable("id") Long id, @RequestBody PayOrderFormDTO payOrderFormDTO){
        payOrderFormDTO.setId(id);
        payOrderService.tryPayOrderByBalance(payOrderFormDTO);
    }

    @ApiOperation("管理员查询所有支付单")
    @GetMapping("/admin/all")
    public PageDTO<PayOrderVO> queryAllPayOrders(@RequestParam(defaultValue = "1") Long current,
                                                  @RequestParam(defaultValue = "10") Long size) {
        Page<PayOrder> page = payOrderService.lambdaQuery()
                .orderByDesc(PayOrder::getCreateTime)
                .page(new Page<>(current, size));
        return PageDTO.of(page, PayOrderVO.class);
    }

    @ApiOperation("根据id查询支付单")
    @GetMapping("/biz/{id}")
    public PayOrderDTO queryPayOrderByBizOrderNo(@PathVariable("id") Long id){
        PayOrder payOrder = payOrderService.lambdaQuery().eq(PayOrder::getBizOrderNo, id).one();
        return BeanUtils.copyBean(payOrder, PayOrderDTO.class);
    }
}
