package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.PaymentMethodCreationRequest;
import com.swp.evchargingstation.dto.response.PaymentMethodResponse;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.PaymentMethod;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.PaymentMethodMapper;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.PaymentMethodRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentMethodService {
    PaymentMethodRepository paymentMethodRepository;
    DriverRepository driverRepository;
    PaymentMethodMapper paymentMethodMapper;

    @Transactional
    public PaymentMethodResponse create(String driverId, PaymentMethodCreationRequest request) {
        log.info("Creating payment method for driver: {}", driverId);

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.DRIVER_NOT_FOUND));

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .driver(driver)
                .methodType(request.getMethodType())
                .provider(request.getProvider())
                .token(request.getToken())
                .build();

        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Payment method created with ID: {}", paymentMethod.getPmId());

        return paymentMethodMapper.toPaymentMethodResponse(paymentMethod);
    }

    public List<PaymentMethodResponse> getMyPaymentMethods(String driverId) {
        log.info("Getting payment methods for driver: {}", driverId);

        List<PaymentMethod> paymentMethods = paymentMethodRepository.findAllByDriverId(driverId);

        return paymentMethods.stream()
                .map(paymentMethodMapper::toPaymentMethodResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(String driverId, String pmId) {
        log.info("Deleting payment method {} for driver: {}", pmId, driverId);

        PaymentMethod paymentMethod = paymentMethodRepository.findById(pmId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        if (!paymentMethod.getDriver().getUserId().equals(driverId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        paymentMethodRepository.delete(paymentMethod);
        log.info("Payment method deleted successfully");
    }
}

