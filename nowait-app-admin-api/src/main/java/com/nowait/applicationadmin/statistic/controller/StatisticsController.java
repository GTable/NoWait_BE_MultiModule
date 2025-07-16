package com.nowait.applicationadmin.statistic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nowait.applicationadmin.order.service.OrderService;
import com.nowait.applicationadmin.statistic.dto.StoreRankingDto;
import com.nowait.applicationadmin.statistic.service.RankingService;
import com.nowait.common.api.ApiUtils;
import com.nowait.domainadminrdb.statistic.dto.OrderSalesSumDetail;
import com.nowait.domaincorerdb.user.entity.MemberDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Statistics API", description = "통계 API")
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

	private final OrderService orderService;
	private final RankingService rankingService;

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
	@Operation(summary = "주점별 매출 통계 조회", description = "주점별 매출 통계를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "주점별 매출 통계 조회 성공")
	public ResponseEntity<?> getTopSalesStores(@AuthenticationPrincipal MemberDetails memberDetails) {
		List<StoreRankingDto> response = rankingService.getStatisticsRankings(memberDetails);
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(
				ApiUtils.success(
					response
				)
			);
	}
}
