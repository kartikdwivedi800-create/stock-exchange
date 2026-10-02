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
                .userId(requestDto.getUserId())
                .currency(requestDto.getCurrency() != null ? requestDto.getCurrency() : "INR")
                .availableBalance(requestDto.getAvailableBalance())
                .build();
    }

    public WalletResponseDto toResponseDto(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return WalletResponseDto.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .availableBalance(wallet.getAvailableBalance())
                .reservedBalance(wallet.getReservedBalance())
                .build();
    }
}
