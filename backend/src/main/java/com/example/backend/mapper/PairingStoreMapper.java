package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.PairingStore;
import org.apache.ibatis.annotations.Mapper;

/**
 * pairing_store 表的数据访问接口。
 */
@Mapper
public interface PairingStoreMapper extends BaseMapper<PairingStore> {
}