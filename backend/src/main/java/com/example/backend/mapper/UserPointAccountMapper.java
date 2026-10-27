package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.UserPointAccount;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserPointAccountMapper extends BaseMapper<UserPointAccount> {
    @Insert("insert ignore into user_point_account (user_id, balance) values (#{userId}, 0)")
    int createIfAbsent(@Param("userId") Long userId);
    @Update("update user_point_account set balance = balance - #{amount} where user_id = #{userId} and balance >= #{amount}")
    int deductIfEnough(@Param("userId") Long userId, @Param("amount") int amount);
    @Update("update user_point_account set balance = balance + #{amount} where user_id = #{userId}")
    int increase(@Param("userId") Long userId, @Param("amount") int amount);
    @Select("select user_id, balance, created_at, updated_at from user_point_account where user_id = #{userId} for update")
    UserPointAccount lockByUserId(@Param("userId") Long userId);
    @Select("<script>select user_id as userId, balance from user_point_account where user_id in <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Map<String, Object>> selectBalancesByUserIds(@Param("userIds") List<Long> userIds);
}
