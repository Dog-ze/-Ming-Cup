package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.UserPointTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPointTransactionMapper extends BaseMapper<UserPointTransaction> { }
