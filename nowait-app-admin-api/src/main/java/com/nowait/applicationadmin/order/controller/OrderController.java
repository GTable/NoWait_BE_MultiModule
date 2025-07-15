package com.nowait.applicationadmin.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.nowait.domaincorerdb.order.dto.OrderSalesSumDetail;
import com.nowait.domaincorerdb.order.dto.TopSalesStoresDetail;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

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
	public ResponseEntity<?> getOrderListByStoreId(@PathVariable Long storeId,
		@AuthenticationPrincipal MemberDetails memberDetails) {
		List<OrderResponseDto> response = orderService.findAllOrders(storeId, memberDetails);
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
		@RequestBody @Valid OrderStatusUpdateRequestDto requestDto,
		@AuthenticationPrincipal MemberDetails memberDetails
	) {
		OrderStatusUpdateResponseDto response = orderService.updateOrderStatus(
			orderId, requestDto.getOrderStatus(), memberDetails);
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(ApiUtils.success(response));
	}

	@GetMapping("/sales")
	@Operation(summary = "오늘의 매출 조회", description = "오늘의 매출을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "오늘의 매출 조회 성공")
	public ResponseEntity<?> getTodaySales(@AuthenticationPrincipal MemberDetails memberDetails) {
		OrderSalesSumDetail sales = orderService.getSaleSumByStoreId(memberDetails);

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					sales
				)
			);
	}

	@GetMapping("/top-sales")
	@Operation(summary = "오늘의 매출 상위 5개 주점 조회", description = "오늘의 매출이 가장 높은 상위 5개 주점을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "오늘의 매출 상위 5개 주점 조회 성공")
	public ResponseEntity<?> getTopSalesStores(@AuthenticationPrincipal MemberDetails memberDetails) {
		List<TopSalesStoresDetail> topSalesStoresDetail =  orderService.getTop5StoresBySalesToday(memberDetails);

		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					topSalesStoresDetail
				)
			);
	}
}
