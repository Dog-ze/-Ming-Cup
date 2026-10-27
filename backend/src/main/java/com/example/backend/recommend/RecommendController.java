package com.example.backend.recommend;

import com.example.backend.common.Result;
import com.example.backend.recommend.vo.RecommendResponseVO;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/recommend")
@AllArgsConstructor
public class RecommendController {
    private final RecommendService recommendService;

    @GetMapping
    public Result<RecommendResponseVO> getRecommend(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude
    ) {
        return Result.ok(recommendService.recommend(city, longitude, latitude));
    }
}