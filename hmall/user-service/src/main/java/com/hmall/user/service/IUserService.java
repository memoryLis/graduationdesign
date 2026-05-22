package com.hmall.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.domain.PageDTO;
import com.hmall.user.domain.dto.LoginFormDTO;
import com.hmall.user.domain.dto.PointRechargeDTO;
import com.hmall.user.domain.dto.RegisterFormDTO;
import com.hmall.user.domain.po.User;
import com.hmall.user.domain.vo.UserLoginVO;
import com.hmall.user.domain.vo.UserManageVO;
import com.hmall.user.domain.vo.UserPointSummaryVO;

public interface IUserService extends IService<User> {

    UserLoginVO login(LoginFormDTO loginFormDTO);

    void deductMoney(String pw, Integer totalFee);

    UserLoginVO register(RegisterFormDTO registerDTO);

    UserPointSummaryVO queryMyPoints();

    PageDTO<UserManageVO> queryUserPage(PageQuery pageQuery, String keyword);

    void freezeUser(Long id);

    void unfreezeUser(Long id);

    void deleteUser(Long id);

    void rechargePoints(Long id, PointRechargeDTO rechargeDTO);

    void changePassword(String oldPassword, String newPassword);

    int sign();
}
