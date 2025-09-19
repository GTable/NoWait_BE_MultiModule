package com.nowait.domaincorerdb.reservation.entity;

import java.time.LocalDateTime;

import com.nowait.common.enums.ReservationStatus;
import com.nowait.domaincorerdb.reservation.exception.InvalidReservationStatusTransitionException;
import com.nowait.domaincorerdb.reservation.exception.ReservationAlreadyCancelledException;
import com.nowait.domaincorerdb.reservation.exception.ReservationAlreadyConfirmedException;
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

	@Column(name = "reservation_number", nullable = true, length = 50)
	private String reservationNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id")
	private Store store;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "updated_at", nullable = true)
	private LocalDateTime updatedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReservationStatus status;

	@Column(name = "party_size", nullable = false)
	private Integer partySize;

	public void markUpdated(LocalDateTime updatedAt, ReservationStatus newStatus) {
		if (this.status == newStatus) {
			switch (newStatus) {
				case CONFIRMED -> throw new ReservationAlreadyConfirmedException();
				case CANCELLED -> throw new ReservationAlreadyCancelledException();
				default -> {}
			}
		}

		if (!isValidTransition(this.status, newStatus)) {
			throw new InvalidReservationStatusTransitionException(this.status, newStatus);
		}
		this.status = newStatus;
		this.updatedAt = updatedAt;
	}

	private boolean isValidTransition(ReservationStatus current, ReservationStatus target) {
		return switch (current) {
			case WAITING -> target == ReservationStatus.CALLING
							|| target == ReservationStatus.CONFIRMED
							|| target == ReservationStatus.CANCELLED;
			case CALLING -> target == ReservationStatus.CONFIRMED
							|| target == ReservationStatus.CANCELLED;
			case CONFIRMED -> target == ReservationStatus.CANCELLED;
			case CANCELLED -> false;
		};
	}
}
