package com.example.userservice.repository;

import com.example.userservice.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Repository
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {
}