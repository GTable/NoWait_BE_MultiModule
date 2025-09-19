package com.nowait.applicationadmin.exception;

import static com.nowait.common.exception.ErrorMessage.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;

import com.nowait.applicationadmin.security.exception.ResourceNotFoundException;
import com.nowait.applicationadmin.security.exception.UnauthorizedException;
import com.nowait.common.exception.ErrorMessage;
import com.nowait.common.exception.ErrorResponse;
import com.nowait.discord.service.DiscordAlarmService;
import com.nowait.domaincorerdb.menu.exception.MenuAlreadyDeletedException;
import com.nowait.domaincorerdb.menu.exception.MenuCreationUnauthorizedException;
import com.nowait.domaincorerdb.menu.exception.MenuCrossStoreConflictException;
import com.nowait.domaincorerdb.menu.exception.MenuDeleteUnauthorizedException;
import com.nowait.domaincorerdb.menu.exception.MenuDuplicateIdException;
import com.nowait.domaincorerdb.menu.exception.MenuImageEmptyException;
import com.nowait.domaincorerdb.menu.exception.MenuInvalidSortOrderException;
import com.nowait.domaincorerdb.menu.exception.MenuNotFoundException;
import com.nowait.domaincorerdb.menu.exception.MenuParamEmptyException;
import com.nowait.domaincorerdb.menu.exception.MenuToggleUnauthorizedException;
import com.nowait.domaincorerdb.menu.exception.MenuUpdateUnauthorizedException;
import com.nowait.domaincorerdb.menu.exception.MenuViewUnauthorizedException;
import com.nowait.domaincorerdb.order.exception.DepositorNameTooLongException;
import com.nowait.domaincorerdb.order.exception.DuplicateOrderException;
import com.nowait.domaincorerdb.order.exception.OrderAlreadyCancelledException;
import com.nowait.domaincorerdb.order.exception.OrderItemsEmptyException;
import com.nowait.domaincorerdb.order.exception.OrderNotFoundException;
import com.nowait.domaincorerdb.order.exception.OrderParameterEmptyException;
import com.nowait.domaincorerdb.order.exception.OrderUpdateUnauthorizedException;
import com.nowait.domaincorerdb.order.exception.OrderViewUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.DuplicateReservationException;
import com.nowait.domaincorerdb.reservation.exception.InvalidReservationParameterException;
import com.nowait.domaincorerdb.reservation.exception.InvalidReservationStatusTransitionException;
import com.nowait.domaincorerdb.reservation.exception.ReservationAddUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationAlreadyCancelledException;
import com.nowait.domaincorerdb.reservation.exception.ReservationAlreadyConfirmedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.reservation.exception.ReservationNumberIssueFailException;
import com.nowait.domaincorerdb.reservation.exception.ReservationUpdateUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationViewUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.UnsupportedReservationStatusException;
import com.nowait.domaincorerdb.reservation.exception.UserWaitingLimitExceededException;
import com.nowait.domaincorerdb.store.exception.StoreDeleteUnauthorizedException;
import com.nowait.domaincorerdb.store.exception.StoreImageEmptyException;
import com.nowait.domaincorerdb.store.exception.StoreImageNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreParamEmptyException;
import com.nowait.domaincorerdb.store.exception.StoreUpdateUnauthorizedException;
import com.nowait.domaincorerdb.store.exception.StoreViewUnauthorizedException;
import com.nowait.domaincorerdb.store.exception.StoreWaitingDisabledException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentAlreadyExistsException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentCreationUnauthorizedException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentNotFoundException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentParamEmptyException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentUpdateUnauthorizedException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentViewUnauthorizedException;
import com.nowait.domaincorerdb.token.exception.BusinessException;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincoreredis.rank.exception.MenuCounterUpdateException;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hidden
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final DiscordAlarmService discordAlarmService;

	// Discord 알림 헬퍼
	private void alarm(Exception e, WebRequest request) {
		discordAlarmService.sendDiscordAdminAlarm(e, request);
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(BusinessException.class)
	public ErrorResponse handleBusinessException(BusinessException e, WebRequest request) {
		alarm(e, request);
		log.error("handleBusinessException", e);
		return new ErrorResponse(e.getMessage(), e.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e, WebRequest request) {
		alarm(e, request);
		log.error("handleMethodArgumentNotValidException", e);
		Map<String, String> errors = getErrors(e);
		return new ErrorResponse(INVALID_INPUT_VALUE.getMessage(), INVALID_INPUT_VALUE.getCode(), errors);
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e, WebRequest request) {
		alarm(e, request);
		log.error("handleHttpMessageNotReadableException", e);
		return new ErrorResponse(INVALID_INPUT_VALUE.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(IllegalArgumentException.class)
	public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
		alarm(e, request);
		log.error("handleIllegalArgumentException", e);
		return new ErrorResponse(e.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MissingRequestValueException.class)
	public ErrorResponse handleMissingRequestValueException(MissingRequestValueException e, WebRequest request) {
		alarm(e, request);
		log.error("handleMissingRequestValueExceptionException", e);
		return new ErrorResponse(INVALID_INPUT_VALUE.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(UNAUTHORIZED)
	@ExceptionHandler(UnauthorizedException.class)
	public ErrorResponse handleUnauthorizedException(UnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("handleUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), e.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(ResourceNotFoundException.class)
	public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("handleResourceNotFoundExceptionException", e);
		return new ErrorResponse(e.getMessage(), e.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MultipartException.class)
	public ErrorResponse handleMultipartException(MultipartException e, WebRequest request) {
		alarm(e, request);
		log.error("handleMultipartException", e);
		return new ErrorResponse(e.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(UserNotFoundException.class)
	public ErrorResponse userNotFoundException(UserNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("userNotFoundException", e);
		return new ErrorResponse(e.getMessage(), NOT_FOUND_USER.getCode());
	}


	/**
	 *  주문 관련 예외 처리
	 */

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(DepositorNameTooLongException.class)
	public ErrorResponse depositorNameTooLongException(DepositorNameTooLongException e, WebRequest request) {
		alarm(e, request);
		log.error("depositorNameTooLongException", e);
		return new ErrorResponse(e.getMessage(), DEPOSITOR_NAME_TOO_LONG.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(DuplicateOrderException.class)
	public ErrorResponse duplicateOrderException(DuplicateOrderException e, WebRequest request) {
		alarm(e, request);
		log.error("duplicateOrderException", e);
		return new ErrorResponse(e.getMessage(), ErrorMessage.DUPLICATE_ORDER.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(OrderAlreadyCancelledException.class)
	public ErrorResponse orderAlreadyCancelledException(OrderAlreadyCancelledException e, WebRequest request) {
		alarm(e, request);
		log.error("orderAlreadyCancelledException", e);
		return new ErrorResponse(e.getMessage(), ORDER_ALREADY_CANCELLED.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(OrderItemsEmptyException.class)
	public ErrorResponse orderItemsEmptyException(OrderItemsEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("orderItemsEmptyException", e);
		return new ErrorResponse(e.getMessage(), ORDER_ITEMS_EMPTY.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(OrderNotFoundException.class)
	public ErrorResponse orderNotFoundException(OrderNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("orderNotFoundException", e);
		return new ErrorResponse(e.getMessage(), ORDER_NOT_FOUND.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(OrderParameterEmptyException.class)
	public ErrorResponse orderParameterEmptyException(OrderParameterEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("orderParameterEmptyException", e);
		return new ErrorResponse(e.getMessage(), ORDER_PARAMETER_EMPTY.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(OrderUpdateUnauthorizedException.class)
	public ErrorResponse orderUpdateUnauthorizedException(OrderUpdateUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("orderUpdateUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), ORDER_UPDATE_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(OrderViewUnauthorizedException.class)
	public ErrorResponse orderViewUnauthorizedException(OrderViewUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("orderViewUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), ORDER_VIEW_UNAUTHORIZED.getCode());
	}

	/**
	 *  예약 관련 예외 처리
	 */

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(DuplicateReservationException.class)
	public ErrorResponse duplicateReservationException(DuplicateReservationException e, WebRequest request) {
		alarm(e, request);
		log.error("duplicateReservationException", e);
		return new ErrorResponse(e.getMessage(), DUPLICATE_RESERVATION.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(InvalidReservationParameterException.class)
	public ErrorResponse invalidReservationParameterException(InvalidReservationParameterException e, WebRequest request) {
		alarm(e, request);
		log.error("invalidReservationParameterException", e);
		return new ErrorResponse(e.getMessage(), INVALID_RESERVATION_PARAMETER.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(InvalidReservationStatusTransitionException.class)
	public ErrorResponse invalidReservationStatusTransitionException(InvalidReservationStatusTransitionException e, WebRequest request) {
		alarm(e, request);
		log.error("invalidReservationStatusTransitionException", e);
		return new ErrorResponse(e.getMessage(), INVALID_RESERVATION_STATUS_TRANSITION.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(ReservationAddUnauthorizedException.class)
	public ErrorResponse reservationAddUnauthorizedException(ReservationAddUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("reservationAddUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), RESERVATION_ADD_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(CONFLICT)
	@ExceptionHandler(ReservationAlreadyCancelledException.class)
	public ErrorResponse reservationAlreadyCancelledException(ReservationAlreadyCancelledException e, WebRequest request) {
		alarm(e, request);
		log.error("reservationAlreadyCancelledException", e);
		return new ErrorResponse(e.getMessage(), RESERVATION_ALREADY_CANCELLED.getCode());
	}

	@ResponseStatus(CONFLICT)
	@ExceptionHandler(ReservationAlreadyConfirmedException.class)
	public ErrorResponse reservationAlreadyConfirmedException(ReservationAlreadyConfirmedException e, WebRequest request) {
		alarm(e, request);
		log.error("reservationAlreadyConfirmedException", e);
		return new ErrorResponse(e.getMessage(), RESERVATION_ALREADY_CONFIRMED.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(ReservationNotFoundException.class)
	public ErrorResponse reservationNotFoundException(ReservationNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("reservationNotFoundException", e);
		return new ErrorResponse(e.getMessage(), NOTFOUND_RESERVATION.getCode());
	}

	@ResponseStatus(INTERNAL_SERVER_ERROR)
	@ExceptionHandler(ReservationNumberIssueFailException.class)
	public ErrorResponse reservationNumberIssueFailException(ReservationNumberIssueFailException e, WebRequest request) {
		alarm(e, request);
		log.error("reservationNumberIssueFailException", e);
		return new ErrorResponse(e.getMessage(), RESERVATION_NUMBER_ISSUE_FAIL.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(ReservationUpdateUnauthorizedException.class)
	public ErrorResponse reservationUpdateUnauthorizedException(ReservationUpdateUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("reservationUpdateUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), RESERVATION_UPDATE_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(ReservationViewUnauthorizedException.class)
	public ErrorResponse reservationViewUnauthorizedException(ReservationViewUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("reservation_viewUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), RESERVATION_VIEW_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(UnsupportedReservationStatusException.class)
	public ErrorResponse unsupportedReservationStatusException(UnsupportedReservationStatusException e, WebRequest request) {
		alarm(e, request);
		log.error("unsupportedReservationStatusException", e);
		return new ErrorResponse(e.getMessage(), UNSUPPORTED_RESERVATION_STATUS.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(UserWaitingLimitExceededException.class)
	public ErrorResponse userWaitingLimitExceededException(UserWaitingLimitExceededException e, WebRequest request) {
		alarm(e, request);
		log.error("userWaitingLimitExceededException", e);
		return new ErrorResponse(e.getMessage(), USER_WAITING_LIMIT_EXCEEDED.getCode());
	}


	/**
	 *  메뉴 관련 예외 처리
	 */
	@ResponseStatus(CONFLICT)
	@ExceptionHandler(MenuAlreadyDeletedException.class)
	public ErrorResponse menuAlreadyDeletedException(MenuAlreadyDeletedException e, WebRequest request) {
		alarm(e, request);
		log.error("menuAlreadyDeletedException", e);
		return new ErrorResponse(e.getMessage(), MENU_ALREADY_DELETED.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(MenuCreationUnauthorizedException.class)
	public ErrorResponse menuCreationUnauthorizedException(MenuCreationUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("menuCreationUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), MENU_CREATION_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(CONFLICT)
	@ExceptionHandler(MenuCrossStoreConflictException.class)
	public ErrorResponse menuCrossStoreConflictException(MenuCrossStoreConflictException e, WebRequest request) {
		alarm(e, request);
		log.error("menuCrossStoreConflictException", e);
		return new ErrorResponse(e.getMessage(), MENU_CROSS_STORE_CONFLICT.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(MenuDeleteUnauthorizedException.class)
	public ErrorResponse menuDeleteUnauthorizedException(MenuDeleteUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("menuDeleteUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), MENU_DELETE_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MenuDuplicateIdException.class)
	public ErrorResponse menuDuplicateIdException(MenuDuplicateIdException e, WebRequest request) {
		alarm(e, request);
		log.error("menuDuplicateIdException", e);
		return new ErrorResponse(e.getMessage(), MENU_DUPLICATE_ID.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MenuImageEmptyException.class)
	public ErrorResponse menuImageEmptyException(MenuImageEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("menuImageEmptyException", e);
		return new ErrorResponse(e.getMessage(), IMAGE_FILE_EMPTY.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(MenuNotFoundException.class)
	public ErrorResponse menuNotFoundException(MenuNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("menuNotFoundException", e);
		return new ErrorResponse(e.getMessage(), MENU_NOT_FOUND.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MenuInvalidSortOrderException.class)
	public ErrorResponse menuInvalidSortOrderException(MenuInvalidSortOrderException e, WebRequest request) {
		alarm(e, request);
		log.error("menuInvalidSortOrderException", e);
		return new ErrorResponse(e.getMessage(), MENU_INVALID_SORT_ORDER.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MenuParamEmptyException.class)
	public ErrorResponse menuParamEmptyException(MenuParamEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("menuParamEmptyException", e);
		return new ErrorResponse(e.getMessage(), MENU_PARAMETER_EMPTY.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(MenuUpdateUnauthorizedException.class)
	public ErrorResponse menuUpdateUnauthorizedException(MenuUpdateUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("menuUpdateUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), MENU_UPDATE_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(MenuToggleUnauthorizedException.class)
	public ErrorResponse menuToggleUnauthorizedException(MenuToggleUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("menuToggleUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), MENU_TOGGLE_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(MenuViewUnauthorizedException.class)
	public ErrorResponse menuViewUnauthorizedException(MenuViewUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("menuViewUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), MENU_VIEW_UNAUTHORIZED.getCode());
	}


	/**
	 *  주점 관련 예외 처리
	 */
	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(StoreDeleteUnauthorizedException.class)
	public ErrorResponse storeDeleteUnauthorizedException(StoreDeleteUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("storeDeleteUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), STORE_DELETE_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(StoreImageEmptyException.class)
	public ErrorResponse storeImageEmptyException(StoreImageEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("storeImageEmptyException", e);
		return new ErrorResponse(e.getMessage(), IMAGE_FILE_EMPTY.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(StoreImageNotFoundException.class)
	public ErrorResponse storeImageNotFoundException(StoreImageNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("storeImageNotFoundException", e);
		return new ErrorResponse(e.getMessage(), IMAGE_FILE_NOT_FOUND.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(StoreNotFoundException.class)
	public ErrorResponse storeNotFoundException(StoreNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("storeNotFoundException", e);
		return new ErrorResponse(e.getMessage(), STORE_NOT_FOUND.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(StoreParamEmptyException.class)
	public ErrorResponse storeParamEmptyException(StoreParamEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("storeParamEmptyException", e);
		return new ErrorResponse(e.getMessage(), STORE_PARAMETER_EMPTY.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(StoreUpdateUnauthorizedException.class)
	public ErrorResponse storeUpdateUnauthorizedException(StoreUpdateUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("storeUpdateUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), STORE_UPDATE_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(StoreViewUnauthorizedException.class)
	public ErrorResponse storeViewUnauthorizedException(StoreViewUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("storeViewUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), STORE_VIEW_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(StoreWaitingDisabledException.class)
	public ErrorResponse storeWaitingDisabledException(StoreWaitingDisabledException e, WebRequest request) {
		alarm(e, request);
		log.error("storeWaitingDisabledException", e);
		return new ErrorResponse(e.getMessage(), STORE_WAITING_DISABLED.getCode());
	}

	/**
	 *  결제 수단 관련 예외처리
	 */
	@ResponseStatus(CONFLICT)
	@ExceptionHandler(StorePaymentAlreadyExistsException.class)
	public ErrorResponse storePaymentAlreadyExistsException(StorePaymentAlreadyExistsException e, WebRequest request) {
		alarm(e, request);
		log.error("storePaymentAlreadyExistsException", e);
		return new ErrorResponse(e.getMessage(), STORE_PAYMENT_ALREADY_EXISTS.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(StorePaymentCreationUnauthorizedException.class)
	public ErrorResponse storePaymentCreationUnauthorizedException(StorePaymentCreationUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("storePaymentCreationUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), STORE_PAYMENT_CREATION_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(StorePaymentNotFoundException.class)
	public ErrorResponse storePaymentNotFoundException(StorePaymentNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("storePaymentNotFoundException", e);
		return new ErrorResponse(e.getMessage(), STORE_PAYMENT_NOT_FOUND.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(StorePaymentParamEmptyException.class)
	public ErrorResponse storePaymentParamEmptyException(StorePaymentParamEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("storePaymentParamEmptyException", e);
		return new ErrorResponse(e.getMessage(), STORE_PAYMENT_PARAMETER_EMPTY.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(StorePaymentUpdateUnauthorizedException.class)
	public ErrorResponse storePaymentUpdateUnauthorizedException(StorePaymentUpdateUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("storePaymentUpdateUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), STORE_PAYMENT_UPDATE_UNAUTHORIZED.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(StorePaymentViewUnauthorizedException.class)
	public ErrorResponse storePaymentViewUnauthorizedException(StorePaymentViewUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("storePaymentViewUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), STORE_PAYMENT_VIEW_UNAUTHORIZED.getCode());
	}

	/**
	 *  redis 관련 예외처리
	 */
	@ResponseStatus(INTERNAL_SERVER_ERROR)
	@ExceptionHandler(MenuCounterUpdateException.class)
	public ErrorResponse menuCounterUpdateException(MenuCounterUpdateException e, WebRequest request) {
		alarm(e, request);
		log.error("menuCounterUpdateException", e);
		return new ErrorResponse(e.getMessage(), MENU_COUNTER_UPDATE.getCode());
	}


	// 공통 에러 Map 생성
	private static Map<String, String> getErrors(MethodArgumentNotValidException e) {
		return e.getBindingResult()
			.getAllErrors()
			.stream()
			.filter(ObjectError.class::isInstance)
			.collect(Collectors.toMap(
				error -> error instanceof FieldError ? ((FieldError)error).getField() : error.getObjectName(),
				ObjectError::getDefaultMessage,
				(msg1, msg2) -> msg1 + ";" + msg2
			));
	}
}
