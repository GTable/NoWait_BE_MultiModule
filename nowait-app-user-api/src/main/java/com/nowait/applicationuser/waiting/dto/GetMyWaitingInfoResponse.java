package com.nowait.applicationuser.waiting.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetMyWaitingInfoResponse {
	private String  reservationId;
	private String publicCode;
	private Long storeId;
	private String storeName;
	private String departmentName;
	private Integer rank;
	private Integer teamsAhead;
	private Integer partySize;
	private String status;
	private LocalDateTime registeredAt;
	private String location;
	private String profileImageUrl;
	private String bannerImageUrl;
}
