package com.nowait.applicationadmin.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationadmin.order.dto.OrderResponseDto;
import com.nowait.applicationadmin.order.dto.OrderStatusUpdateRequestDto;
import com.nowait.applicationadmin.order.dto.OrderStatusUpdateResponseDto;
import com.nowait.applicationadmin.order.service.OrderService;
import com.nowait.common.api.ApiUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Order API", description = "주문 API")
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{storeId}")
    @Operation(summary = "주점별 주문리스트 조회", description = "특정 주점에 대한 예약리스트 조회")
    @ApiResponse(responseCode = "200", description = "주리스트 조회")
    public ResponseEntity<?> getOrderListByStoreId(@PathVariable Long storeId) {
        List<OrderResponseDto> response = orderService.findAllOrders(storeId);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                ApiUtils.success(
                    response
                )
            );
    }

    @PatchMapping("/status/{orderId}")
    @Operation(summary = "주문 상태 변경", description = "특정 주문의 상태를 변경.")
    @ApiResponse(responseCode = "200", description = "주문 상태 변경 성공")
    @ApiResponse(responseCode = "400", description = "주문을 찾을 수 없음")
    public ResponseEntity<?> updateOrderStatus(
        @PathVariable Long orderId,
        @RequestBody@Valid OrderStatusUpdateRequestDto requestDto
    ) {
        OrderStatusUpdateResponseDto response = orderService.updateOrderStatus(orderId, requestDto.getOrderStatus());
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiUtils.success(response));
    }
}
