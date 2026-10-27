package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Questionnaire;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionnaireMapper extends BaseMapper<Questionnaire> {
}