package com.hmall.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.user.domain.po.Address;
import com.hmall.user.mapper.AddressMapper;
import com.hmall.user.service.IAddressService;
import org.springframework.stereotype.Service;

/**
 * ClassName: AddressServiceImpl
 * Package: com.hmall.user.service.impl
 * Description:
 *
 * @Author liang
 * @Create 2025/10/30 14:07
 * @Version jdk17.0
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService{

}
