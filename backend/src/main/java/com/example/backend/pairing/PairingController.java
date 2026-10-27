package com.example.backend.pairing;

import com.example.backend.common.Result;
import com.example.backend.pairing.vo.PairingVO;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pairing")
@AllArgsConstructor
public class PairingController {
    private final PairingService pairingService;

    @GetMapping
    public Result<PairingVO> getPairing(
            @RequestParam(required = false) String teaCategory
    ) {
        return Result.ok(pairingService.getPairing(teaCategory));
    }
}