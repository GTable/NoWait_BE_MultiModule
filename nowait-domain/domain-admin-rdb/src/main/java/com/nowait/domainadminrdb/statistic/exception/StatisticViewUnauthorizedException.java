package com.nowait.domainadminrdb.statistic.exception;

import com.nowait.common.exception.ErrorMessage;

public class StatisticViewUnauthorizedException extends RuntimeException {
	public StatisticViewUnauthorizedException() {
		super(ErrorMessage.STATISTIC_VIEW_UNAUTHORIZED.getMessage());
	}
}
