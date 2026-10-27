package com.example.backend.record;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.BizException;
import com.example.backend.entity.TeaRecord;
import com.example.backend.mapper.TeaRecordMapper;
import com.example.backend.record.dto.TeaRecordSaveRequest;
import com.example.backend.record.vo.PageVO;
import com.example.backend.record.vo.TeaRecordVO;
import com.example.backend.point.PointService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;

@Service
public class TeaRecordService {
    private final TeaRecordMapper teaRecordMapper;
    private final ObjectMapper objectMapper;
    private final PointService pointService;

    public TeaRecordService(TeaRecordMapper teaRecordMapper, ObjectMapper objectMapper) {
        this(teaRecordMapper, objectMapper, null);
    }
    @Autowired
    public TeaRecordService(TeaRecordMapper teaRecordMapper, ObjectMapper objectMapper, PointService pointService) {
        this.teaRecordMapper = teaRecordMapper; this.objectMapper = objectMapper; this.pointService = pointService;
    }

    /**
     * 新增一条品茶记录
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public TeaRecordVO create(Long userId, TeaRecordSaveRequest request) {
        TeaRecord entity = new TeaRecord();
        entity.setUserId(userId);
        copyRequest(request, entity);
        teaRecordMapper.insert(entity);
        if (pointService != null) pointService.rewardDailyTeaRecord(userId, entity.getId());
        return TeaRecordVO.from(entity, objectMapper);
    }

    /**
     * 分页查询某个用户自己的品茶记录。
     * @param userId
     * @param page
     * @param pageSize
     * @return
     */
    @Transactional(readOnly = true)
    public PageVO<TeaRecordVO> list(Long userId, long page, long pageSize) {
        Page<TeaRecord> result = teaRecordMapper.selectPage(
                Page.of(page, pageSize),
                new LambdaQueryWrapper<TeaRecord>()
                        .eq(TeaRecord::getUserId, userId)
                        .orderByDesc(TeaRecord::getCreatedAt)
                        .orderByDesc(TeaRecord::getId)
        );
        List<TeaRecordVO> records = result.getRecords().stream()
                .map(item -> TeaRecordVO.from(item, objectMapper))
                .toList();
        return new PageVO<>(records, page, pageSize, result.getTotal());
    }

    /**
     * 获取某条品茶记录的详情。
     * @param userId
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public TeaRecordVO getDetail(Long userId, Long id) {
        return TeaRecordVO.from(getOwnedRecord(userId, id), objectMapper);
    }

    /**
     * 更新某条品茶记录。
     * @param userId
     * @param id
     * @param request
     * @return
     */
    @Transactional
    public TeaRecordVO update(Long userId, Long id, TeaRecordSaveRequest request) {
        TeaRecord entity = getOwnedRecord(userId, id);
        copyRequest(request, entity);
        updateRecord(entity);
        return TeaRecordVO.from(entity, objectMapper);
    }
    private void updateRecord(TeaRecord entity) {
        teaRecordMapper.update(null, new LambdaUpdateWrapper<TeaRecord>()
                .eq(TeaRecord::getId, entity.getId())
                .set(TeaRecord::getTeaName, entity.getTeaName())
                .set(TeaRecord::getCategory, entity.getCategory())
                .set(TeaRecord::getBrand, entity.getBrand())
                .set(TeaRecord::getAcquisitionMethod, entity.getAcquisitionMethod())
                .set(TeaRecord::getPrice, entity.getPrice())
                .set(TeaRecord::getBrewTemp, entity.getBrewTemp())
                .set(TeaRecord::getRating, entity.getRating())
                .set(TeaRecord::getComment, entity.getComment())
                .set(TeaRecord::getImages, entity.getImages()));
    }
    /**
     * 删除某条品茶记录。
     * @param userId
     * @param id
     * @return
     */
    @Transactional
    public boolean delete(Long userId, Long id) {
        TeaRecord entity = getOwnedRecord(userId, id);
        return teaRecordMapper.deleteById(entity.getId()) == 1;
    }

    /**
     * 获取某条品茶记录，并检查是否属于当前用户。
     * @param userId
     * @param id
     * @return
     */
    private TeaRecord getOwnedRecord(Long userId, Long id) {
        TeaRecord entity = teaRecordMapper.selectOne(
                new LambdaQueryWrapper<TeaRecord>()
                        .eq(TeaRecord::getId, id)
                        .eq(TeaRecord::getUserId, userId)
        );
        if (entity != null) return entity;

        if (teaRecordMapper.selectById(id) == null) {
            throw new BizException(404, "品茶记录不存在");
        }
        throw new BizException(403, "无权操作该品茶记录");
    }

    /**
     * 将请求对象的字段复制到实体对象中。
     * @param request
     * @param entity
     */
    private void copyRequest(TeaRecordSaveRequest request, TeaRecord entity) {
        entity.setTeaName(request.teaName().trim());
        entity.setCategory(request.category());
        entity.setBrand(blankToNull(request.brand()));
        entity.setAcquisitionMethod(request.acquisitionMethod());
        entity.setPrice(request.price());
        entity.setBrewTemp(request.brewTemp());
        entity.setRating(request.rating());
        entity.setComment(blankToNull(request.comment()));
        entity.setImages(toJson(request.images()));
    }

    /**
     * 将空字符串或仅包含空白字符的字符串转换为 null，否则返回去掉首尾空白的字符串。
     * @param value
     * @return
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 将图片地址列表转换为 JSON 字符串。
     * @param images
     * @return
     */
    private String toJson(List<String> images) {
        if (images == null || images.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JacksonException exception) {
            throw new BizException(400, "图片地址格式不正确");
        }
    }
}
