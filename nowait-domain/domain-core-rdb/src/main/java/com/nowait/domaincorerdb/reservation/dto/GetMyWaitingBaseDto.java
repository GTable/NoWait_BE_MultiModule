package com.nowait.domaincorerdb.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.nowait.common.enums.ReservationStatus;
import com.querydsl.core.annotations.QueryProjection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class GetMyWaitingBaseDto {
	private final String reservationId;
	private final String publicCode;
	private final Long storeId;
	private final String storeName;
	private final String departmentName;
	private final Integer partySize;
	private final ReservationStatus status;
	private final LocalDateTime registeredAt;
	private final String location;
	private final String profileImageUrl;
	private final String bannerImageUrl;

	@QueryProjection
	public GetMyWaitingBaseDto(
		String reservationId,
		String publicCode,
		Long storeId,
		String storeName,
		String departmentName,
		Integer partySize,
		ReservationStatus status,
		LocalDateTime registeredAt,
		String location,
		String profileImageUrl,
		String bannerImageUrl
	) {
		this.reservationId = reservationId;
		this.publicCode = publicCode;
		this.storeId = storeId;
		this.storeName = storeName;
		this.departmentName = departmentName;
		this.partySize = partySize;
		this.status = status;
		this.registeredAt = registeredAt;
		this.location = location;
		this.profileImageUrl = profileImageUrl == null ? "" : profileImageUrl;
		this.bannerImageUrl = bannerImageUrl == null ? "" : bannerImageUrl;
	}
}
