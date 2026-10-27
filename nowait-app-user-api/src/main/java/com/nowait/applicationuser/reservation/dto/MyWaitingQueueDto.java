package com.nowait.applicationuser.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 대기 큐 정보 DTO")
public class MyWaitingQueueDto {
	@Schema(description = "예약 ID", example = "1-20240720-0001")
	private String  reservationId;
	@Schema(description = "주점 ID", example = "1")
	private Long storeId;
	@Schema(description = "주점 이름", example = "비어파티")
	private String storeName;
	@Schema(description = "학과 이름", example = "경영학과")
	private String departmentName;
	@Schema(description = "대기 순번", example = "1")
	private Integer rank;
	@Schema(description = "내 앞의 대기 팀 수", example = "3")
	private Integer teamsAhead;
	@Schema(description = "대기 인원(파티 사이즈)", example = "4")
	private Integer partySize;   // 파티 인원
	@Schema(description = "대기 상태", example = "WAITING")
	private String status;
	@Schema(description = "대기 등록 일시", example = "2024-07-20T18:00:00")
	private LocalDateTime registeredAt;
	@Schema(description = "주점 위치", example = "학생회관 1층 104호")
	private String location;
	@Schema(description = "프로필 이미지 URL", example = "https://cdn.gtable.com/profile/user1.jpg")
	private String profileImageUrl;
	@Schema(description = "배너 이미지 URL", example = "https://cdn.gtable.com/profile/user1.jpg")
	private List<String> bannerImageUrl;
}

