package com.nexusexchange.wallet.controller;

import com.nexusexchange.wallet.dto.request.WalletRequestDto;
import com.nexusexchange.wallet.dto.response.WalletResponseDto;
import com.nexusexchange.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponseDto> createWallet(@RequestBody WalletRequestDto requestDto) {
        return ResponseEntity.ok(walletService.createWallet(requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponseDto> getWallet(@PathVariable Long id) {
        return ResponseEntity.ok(walletService.getWallet(id));
    }
}
