package com.example.backend.location;

import com.example.backend.common.Result;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/location")
@AllArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @GetMapping("/reverse")
    public Result<String> reverse(
            @RequestParam BigDecimal longitude,
            @RequestParam BigDecimal latitude
    ) {
        return Result.ok(locationService.reverseGeocode(longitude, latitude));
    }
}
