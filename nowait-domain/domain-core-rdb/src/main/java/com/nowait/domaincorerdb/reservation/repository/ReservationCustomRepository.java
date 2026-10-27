package com.nowait.domaincorerdb.reservation.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.reservation.dto.GetMyWaitingBaseDto;

@Repository
public interface ReservationCustomRepository {
	List<GetMyWaitingBaseDto> findMyWaitingInfo(Long userId);
}
