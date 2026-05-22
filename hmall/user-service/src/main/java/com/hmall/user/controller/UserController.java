package com.hmall.user.controller;

import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.user.domain.dto.ChangePasswordDTO;
import com.hmall.user.domain.dto.LoginFormDTO;
import com.hmall.user.domain.dto.PointRechargeDTO;
import com.hmall.user.domain.dto.RegisterFormDTO;
import com.hmall.user.domain.vo.UserLoginVO;
import com.hmall.user.domain.vo.UserManageVO;
import com.hmall.user.domain.vo.UserPointSummaryVO;
import com.hmall.user.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @ApiOperation("用户登录接口")
    @PostMapping("login")
    public UserLoginVO login(@RequestBody @Validated LoginFormDTO loginFormDTO) {
        return userService.login(loginFormDTO);
    }

    @ApiOperation("用户注册接口")
    @PostMapping("register")
    public UserLoginVO register(@RequestBody @Validated RegisterFormDTO registerFormDTO) {
        return userService.register(registerFormDTO);
    }

    @ApiOperation("扣减积分")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pw", value = "支付密码"),
            @ApiImplicitParam(name = "amount", value = "支付积分")
    })
    @PutMapping("/money/deduct")
    public void deductMoney(@RequestParam("pw") String pw, @RequestParam("amount") Integer amount) {
        userService.deductMoney(pw, amount);
    }

    @ApiOperation("查询当前用户积分余额")
    @GetMapping("/me/points")
    public UserPointSummaryVO queryMyPoints() {
        return userService.queryMyPoints();
    }

    @ApiOperation("管理员查询用户列表")
    @GetMapping("/admin/list")
    public PageDTO<UserManageVO> queryUserPage(PageQuery pageQuery,
                                               @RequestParam(value = "keyword", required = false) String keyword) {
        return userService.queryUserPage(pageQuery, keyword);
    }

    @ApiOperation("管理员冻结用户")
    @PutMapping("/admin/{id}/freeze")
    public void freezeUser(@PathVariable("id") Long id) {
        userService.freezeUser(id);
    }

    @ApiOperation("管理员解冻用户")
    @PutMapping("/admin/{id}/unfreeze")
    public void unfreezeUser(@PathVariable("id") Long id) {
        userService.unfreezeUser(id);
    }

    @ApiOperation("管理员删除用户")
    @DeleteMapping("/admin/{id}")
    public void deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
    }

    @ApiOperation("管理员给用户充值积分")
    @PostMapping("/admin/{id}/points/recharge")
    public void rechargePoints(@PathVariable("id") Long id,
                               @RequestBody @Validated PointRechargeDTO rechargeDTO) {
        userService.rechargePoints(id, rechargeDTO);
    }

    @ApiOperation("修改密码")
    @PutMapping("/password")
    public void changePassword(@RequestBody @Validated ChangePasswordDTO dto) {
        userService.changePassword(dto.getOldPassword(), dto.getNewPassword());
    }

    @ApiOperation("每日签到")
    @PostMapping("/sign")
    public Map<String, Object> sign() {
        try {
            int points = userService.sign();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("points", points);
            result.put("message", "签到成功，获得" + points + "积分");
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }
}
