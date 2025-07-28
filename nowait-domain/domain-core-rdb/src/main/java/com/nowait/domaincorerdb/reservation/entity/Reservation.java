package com.nowait.domaincorerdb.reservation.entity;

import java.time.LocalDateTime;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "reservation_number", nullable = false, length = 50)
	private String reservationNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id")
	private Store store;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "called_at", nullable = true)
	private LocalDateTime calledAt;      // 호출 시각

	@Column(name = "confirmed_at", nullable = true)
	private LocalDateTime confirmedAt;   // 확정(입장 완료) 시각

	@Column(name = "cancelled_at", nullable = true)
	private LocalDateTime cancelledAt;   // 취소 시각

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus status;

	@Column(name = "party_size", nullable = false)
	private Integer partySize;

	public void updateStatus(ReservationStatus status) {
		this.status = status;
	}

	// 상태 전환 메서드
	public void markCalling(LocalDateTime ts) {
		this.status   = ReservationStatus.CALLING;
		this.calledAt = ts;
	}

	public void markConfirmed(LocalDateTime ts) {
		this.status      = ReservationStatus.CONFIRMED;
		this.confirmedAt = ts;
	}

	public void markCancelled(LocalDateTime ts) {
		this.status      = ReservationStatus.CANCELLED;
		this.cancelledAt = ts;
	}
}
