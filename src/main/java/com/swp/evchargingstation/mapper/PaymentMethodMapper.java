package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.response.PaymentMethodResponse;
import com.swp.evchargingstation.entity.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {
    @Mapping(target = "maskedToken", expression = "java(maskToken(paymentMethod.getToken()))")
    PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod);

    default String maskToken(String token) {
        if (token == null || token.length() <= 4) {
            return "****";
        }
        return "**** **** **** " + token.substring(token.length() - 4);
    }
}