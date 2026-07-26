package com.nexusexchange.wallet.service;

import com.nexusexchange.wallet.dto.request.WalletRequestDto;
import com.nexusexchange.wallet.dto.response.WalletResponseDto;

public interface WalletService {
    WalletResponseDto createWallet(WalletRequestDto requestDto);
    WalletResponseDto getWallet(Long id);
}
