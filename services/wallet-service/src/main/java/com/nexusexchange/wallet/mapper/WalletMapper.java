package com.nexusexchange.wallet.mapper;

import com.nexusexchange.wallet.dto.request.WalletRequestDto;
import com.nexusexchange.wallet.dto.response.WalletResponseDto;
import com.nexusexchange.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public Wallet toEntity(WalletRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }
        return Wallet.builder()
                .currency(requestDto.getCurrency())
                .balance(requestDto.getBalance())
                .build();
    }

    public WalletResponseDto toResponseDto(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return WalletResponseDto.builder()
                .id(wallet.getId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .build();
    }
}
