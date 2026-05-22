package com.hmall.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.exception.ForbiddenException;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.common.utils.UserContext;
import com.hmall.user.config.JwtProperties;
import com.hmall.user.domain.dto.LoginFormDTO;
import com.hmall.user.domain.dto.PointRechargeDTO;
import com.hmall.user.domain.dto.RegisterFormDTO;
import com.hmall.user.domain.po.Address;
import com.hmall.user.domain.po.User;
import com.hmall.user.domain.vo.UserLoginVO;
import com.hmall.user.domain.vo.UserManageVO;
import com.hmall.user.domain.vo.UserPointSummaryVO;
import com.hmall.user.enums.UserStatus;
import com.hmall.user.mapper.AddressMapper;
import com.hmall.user.mapper.UserMapper;
import com.hmall.user.service.IUserService;
import com.hmall.user.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final String ADMIN_USERNAME = "123";

    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;
    private final AddressMapper addressMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UserLoginVO login(LoginFormDTO loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        User user = lambdaQuery().eq(User::getUsername, username).one();
        Assert.notNull(user, "用户名错误");
        if (user.getStatus() == UserStatus.FROZEN) {
            throw new ForbiddenException("用户已被冻结");
        }
        if (!user.getPassword().equals(password)) {
            throw new BadRequestException("用户名或密码错误");
        }

        String token = jwtTool.createToken(user.getId(), user.getUsername(), jwtProperties.getTokenTTL());
        return new UserLoginVO()
                .setUserId(user.getId())
                .setUsername(user.getUsername())
                .setBalance(defaultBalance(user.getBalance()))
                .setToken(token);
    }

    @Override
    public void deductMoney(String pw, Integer totalFee) {
        log.info("开始扣款");
        User user = getById(UserContext.getUser());
        if (user == null || !user.getPassword().equals(pw)) {
            throw new BizIllegalException("用户密码错误");
        }

        int updated = baseMapper.updateMoney(UserContext.getUser(), totalFee);
        if (updated == 0) {
            throw new RuntimeException("扣款失败，可能是积分不足");
        }
        log.info("扣款成功");
    }

    @Override
    public UserLoginVO register(RegisterFormDTO registerDTO) {
        long count = lambdaQuery().eq(User::getUsername, registerDTO.getUsername()).count();
        if (count > 0) {
            throw new BadRequestException("用户名已存在");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(registerDTO.getPassword());
        user.setPhone(registerDTO.getPhone());
        user.setStatus(UserStatus.NORMAL);
        user.setBalance(100000);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);

        String token = jwtTool.createToken(user.getId(), user.getUsername(), jwtProperties.getTokenTTL());
        return new UserLoginVO()
                .setUserId(user.getId())
                .setUsername(user.getUsername())
                .setBalance(defaultBalance(user.getBalance()))
                .setToken(token);
    }

    @Override
    public UserPointSummaryVO queryMyPoints() {
        User user = getCurrentUser();
        return new UserPointSummaryVO()
                .setUserId(user.getId())
                .setUsername(user.getUsername())
                .setPoints(defaultBalance(user.getBalance()));
    }

    @Override
    public PageDTO<UserManageVO> queryUserPage(PageQuery pageQuery, String keyword) {
        assertAdmin();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(User::getUsername, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        Page<User> page = page(pageQuery.toMpPageDefaultSortByCreateTimeDesc(), queryWrapper);
        return PageDTO.of(page, this::toUserManageVO);
    }

    @Override
    public void freezeUser(Long id) {
        updateUserStatus(id, UserStatus.FROZEN);
    }

    @Override
    public void unfreezeUser(Long id) {
        updateUserStatus(id, UserStatus.NORMAL);
    }

    @Override
    public void deleteUser(Long id) {
        assertAdmin();
        User targetUser = getUserOrThrow(id);
        if (isAdmin(targetUser)) {
            throw new BizIllegalException("管理员账号不可删除");
        }
        removeById(id);
        addressMapper.delete(new LambdaQueryWrapper<Address>().eq(Address::getUserId, id));
    }

    @Override
    public void rechargePoints(Long id, PointRechargeDTO rechargeDTO) {
        assertAdmin();
        User targetUser = getUserOrThrow(id);
        int latestBalance = defaultBalance(targetUser.getBalance()) + rechargeDTO.getAmount();
        targetUser.setBalance(latestBalance);
        targetUser.setUpdateTime(LocalDateTime.now());
        updateById(targetUser);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        User user = getCurrentUser();
        if (!user.getPassword().equals(oldPassword)) {
            throw new BadRequestException("原密码错误");
        }
        user.setPassword(newPassword);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
    }

    @Override
    public int sign() {
        Long userId = UserContext.getUser();
        String today = LocalDate.now().toString();
        String signKey = "sign:" + userId + ":" + today;

        Boolean alreadySigned = stringRedisTemplate.hasKey(signKey);
        if (Boolean.TRUE.equals(alreadySigned)) {
            throw new BadRequestException("今日已签到，请明天再来");
        }

        int points = 50;
        User user = getCurrentUser();
        user.setBalance(defaultBalance(user.getBalance()) + points);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);

        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        Duration duration = Duration.between(LocalDateTime.now(), endOfDay);
        stringRedisTemplate.opsForValue().set(signKey, "1", duration);

        return points;
    }

    private void updateUserStatus(Long id, UserStatus status) {
        assertAdmin();
        User targetUser = getUserOrThrow(id);
        if (isAdmin(targetUser)) {
            throw new BizIllegalException("管理员账号不可冻结");
        }
        targetUser.setStatus(status);
        targetUser.setUpdateTime(LocalDateTime.now());
        updateById(targetUser);
    }

    private User getCurrentUser() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new UnauthorizedException("请先登录");
        }
        return getUserOrThrow(userId);
    }

    private User getUserOrThrow(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        return user;
    }

    private void assertAdmin() {
        User currentUser = getCurrentUser();
        if (!isAdmin(currentUser)) {
            throw new ForbiddenException("仅管理员可操作");
        }
    }

    private boolean isAdmin(User user) {
        return ADMIN_USERNAME.equals(user.getUsername());
    }

    private int defaultBalance(Integer balance) {
        return balance == null ? 0 : balance;
    }

    private UserManageVO toUserManageVO(User user) {
        return new UserManageVO()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setPhone(user.getPhone())
                .setStatus(user.getStatus() == null ? null : user.getStatus().getValue())
                .setStatusText(user.getStatus() == null ? "" : user.getStatus().getDesc())
                .setPoints(defaultBalance(user.getBalance()))
                .setCreateTime(user.getCreateTime());
    }
}
