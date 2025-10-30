package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.dto.UserDTO;
import com.example.common.enumz.ErrorCode;
import com.example.common.util.RedisLockUtil;
import com.example.common.entity.Result;
import com.example.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务
 */
@Service
@Slf4j
public class UserService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private RedisLockUtil redisLockUtil;

    /**
     * 根据ID获取用户
     */
    public Result<UserDTO> getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), "用户不存在");
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        return Result.success(userDTO);
    }

    /**
     * 获取所有用户
     */
    public Result<List<UserDTO>> getAllUsers() {
        List<User> users = userRepository.selectList(null);
        List<UserDTO> userDTOs = users.stream().map(user -> {
            UserDTO dto = new UserDTO();
            BeanUtils.copyProperties(user, dto);
            return dto;
        }).collect(Collectors.toList());
        return Result.success(userDTOs);
    }

    /**
     * 创建用户
     */
    public Result<UserDTO> createUser(User user) {
        boolean success = userRepository.insert(user) > 0;
        if (success) {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            return Result.success(userDTO);
        }
        return Result.fail("创建用户失败");
    }

    /**
     * 更新用户
     */
    public Result<UserDTO> updateUser(User user) {
        boolean success = userRepository.updateById(user) > 0;
        if (success) {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            return Result.success(userDTO);
        }
        return Result.fail("更新用户失败");
    }

    /**
     * 删除用户
     */
    public Result<Boolean> deleteUser(Long id) {
        boolean success = userRepository.deleteById(id) > 0;
        return success ? Result.success(true) : Result.fail("删除用户失败");
    }

    /**
     * 扣减用户余额（使用分布式锁）
     */
    @Transactional
    public Result<Boolean> deductBalance(Long userId, Integer amount) {
        // 生成锁的key
        String lockKey = "deduct_balance:" + userId;
        String lockValue = null;
        try {
            // 尝试获取分布式锁，最多等待3秒
            lockValue = redisLockUtil.tryLock(lockKey, 10, 3);
            if (lockValue == null) {
                log.warn("获取分布式锁失败: {}", lockKey);
                return Result.fail(ErrorCode.LOCK_FAIL.getCode(), "获取分布式锁失败");
            }

            // 获取用户信息
            User user = userRepository.selectById(userId);
            if (user == null) {
                return Result.fail(ErrorCode.NOT_FOUND.getCode(), "用户不存在");
            }

            // 检查余额是否充足
            if (user.getBalance() < amount) {
                return Result.fail("余额不足");
            }

            // 扣减余额
            user.setBalance(user.getBalance() - amount);
            boolean success = userRepository.updateById(user) > 0;
            return success ? Result.success(true) : Result.fail("扣减余额失败");
        } catch (InterruptedException e) {
            log.error("获取分布式锁被中断", e);
            Thread.currentThread().interrupt();
            return Result.fail("操作被中断");
        } finally {
            // 释放锁
            if (lockValue != null) {
                redisLockUtil.unlock(lockKey, lockValue);
            }
        }
    }
}