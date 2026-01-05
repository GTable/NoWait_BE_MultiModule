package com.nowait.applicationuser.exception;

import static com.nowait.common.exception.ErrorMessage.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;

import com.nowait.applicationuser.security.exception.ResourceNotFoundException;
import com.nowait.applicationuser.security.exception.UnauthorizedException;
import com.nowait.common.exception.ErrorMessage;
import com.nowait.common.exception.ErrorResponse;
import com.nowait.discord.service.DiscordAlarmService;
import com.nowait.domaincorerdb.order.exception.DepositorNameTooLongException;
import com.nowait.domaincorerdb.order.exception.DuplicateOrderException;
import com.nowait.domaincorerdb.order.exception.OrderItemsEmptyException;
import com.nowait.domaincorerdb.order.exception.OrderParameterEmptyException;
import com.nowait.domaincorerdb.reservation.exception.DuplicateReservationException;
import com.nowait.domaincorerdb.reservation.exception.ReservationAddUnauthorizedException;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.reservation.exception.ReservationNumberIssueFailException;
import com.nowait.domaincorerdb.reservation.exception.UserWaitingLimitExceededException;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreWaitingDisabledException;
import com.nowait.domaincorerdb.storepayment.exception.StorePaymentNotFoundException;
import com.nowait.domaincorerdb.token.exception.BusinessException;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domainuserrdb.bookmark.exception.AlreadyDeletedBookmarkException;
import com.nowait.domainuserrdb.bookmark.exception.BookmarkNotFoundException;
import com.nowait.domainuserrdb.bookmark.exception.BookmarkOwnerMismatchException;
import com.nowait.domainuserrdb.bookmark.exception.DuplicateBookmarkException;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hidden
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

	private final DiscordAlarmService discordAlarmService;

	// Discord 알림 헬퍼
	private void alarm(Exception e, WebRequest request) {
		discordAlarmService.sendDiscordUserAlarm(e, request);
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(OAuth2AuthenticationException.class)
	public ErrorResponse handlerOAuth2AuthenticationException(OAuth2AuthenticationException e, WebRequest request) {
		alarm(e, request);
		log.error("handleOAuth2AuthenticationException", e);
		return new ErrorResponse("OAuth 인증 실패 : " + e.getMessage(), INVALID_INPUT_VALUE.getCode());
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
		log.error("handleMissingRequestValueException", e);
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
		log.error("handleResourceNotFoundException", e);
		return new ErrorResponse(e.getMessage(), e.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(MultipartException.class)
	public ErrorResponse handleMultipartException(MultipartException e, WebRequest request) {
		alarm(e, request);
		log.error("handleMultipartException", e);
		return new ErrorResponse(e.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(DuplicateBookmarkException.class)
	public ErrorResponse handleDuplicateBookmarkException(DuplicateBookmarkException e, WebRequest request) {
		alarm(e, request);
		log.error("handleDuplicateBookmarkException", e);
		return new ErrorResponse(e.getMessage(), ErrorMessage.DUPLICATE_BOOKMARK.getCode());
	}

	@ResponseStatus(FORBIDDEN)
	@ExceptionHandler(BookmarkOwnerMismatchException.class)
	public ErrorResponse bookmarkOwnerMismatchException(BookmarkOwnerMismatchException e, WebRequest request) {
		alarm(e, request);
		log.error("bookmarkOwnerMismatchException", e);
		return new ErrorResponse(e.getMessage(), NOT_OWN_BOOKMARK.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(BookmarkNotFoundException.class)
	public ErrorResponse handleBookmarkNotFoundException(
		BookmarkNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("handleBookmarkNotFoundException", e);
		return new ErrorResponse(e.getMessage(), NOT_FOUND_BOOKMARK.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(AlreadyDeletedBookmarkException.class)
	public ErrorResponse handleAlreadyDeleteBookmark(
		AlreadyDeletedBookmarkException e, WebRequest request) {
		alarm(e, request);
		log.error("AlreadyDeletedBookmarkException", e);
		return new ErrorResponse(e.getMessage(), ALREADY_DELETED_BOOKMARK.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(UserNotFoundException.class)
	public ErrorResponse userNotFoundException(UserNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("userNotFoundException", e);
		return new ErrorResponse(e.getMessage(), NOT_FOUND_USER.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(OrderParameterEmptyException.class)
	public ErrorResponse orderParameterEmptyException(OrderParameterEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("orderParameterEmptyException", e);
		return new ErrorResponse(e.getMessage(), ORDER_PARAMETER_EMPTY.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(OrderItemsEmptyException.class)
	public ErrorResponse orderItemsEmptyException(OrderItemsEmptyException e, WebRequest request) {
		alarm(e, request);
		log.error("orderItemsEmptyException", e);
		return new ErrorResponse(e.getMessage(), ORDER_ITEMS_EMPTY.getCode());
	}

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

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(ReservationNotFoundException.class)
	public ErrorResponse reservationNotFoundException(ReservationNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("reservationNotFoundException", e);
		return new ErrorResponse(e.getMessage(), NOTFOUND_RESERVATION.getCode());
	}

	@ResponseStatus(CONFLICT)
	@ExceptionHandler(DuplicateReservationException.class)
	public ErrorResponse duplicateReservationException(DuplicateReservationException e, WebRequest request) {
		alarm(e, request);
		log.error("duplicateReservationException", e);
		return new ErrorResponse(e.getMessage(), DUPLICATE_RESERVATION.getCode());
	}

	@ResponseStatus(BAD_REQUEST)
	@ExceptionHandler(StoreWaitingDisabledException.class)
	public ErrorResponse storeWaitingDisabledException(StoreWaitingDisabledException e, WebRequest request) {
		alarm(e, request);
		log.error("storeWaitingDisabledException", e);
		return new ErrorResponse(e.getMessage(), STORE_WAITING_DISABLED.getCode());
	}

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(StoreNotFoundException.class)
	public ErrorResponse handleStoreNotFoundException(StoreNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("handleStoreNotFoundException", e);
		return new ErrorResponse(e.getMessage(), STORE_NOT_FOUND.getCode());
	}

	@ResponseStatus(NO_CONTENT)
	@ExceptionHandler(StorePaymentNotFoundException.class)
	public ErrorResponse handleStorePaymentNotFoundException(StorePaymentNotFoundException e, WebRequest request) {
		alarm(e, request);
		log.error("handleStorePaymentNotFoundException", e);
		return new ErrorResponse(e.getMessage(), STORE_PAYMENT_NOT_FOUND.getCode());
	}

	@ResponseStatus(CONFLICT)
	@ExceptionHandler(UserWaitingLimitExceededException.class)
	public ErrorResponse handleUserWaitingLimitExceededException(
		UserWaitingLimitExceededException e, WebRequest request) {
		alarm(e, request);
		log.error("handleUserWaitingLimitExceededException", e);
		return new ErrorResponse(e.getMessage(), USER_WAITING_LIMIT_EXCEEDED.getCode());
	}

	@ResponseStatus(INTERNAL_SERVER_ERROR)
	@ExceptionHandler(ReservationNumberIssueFailException.class)
	public ErrorResponse handleReservationNumberIssueFailException(
		ReservationNumberIssueFailException e, WebRequest request) {
		alarm(e, request);
		log.error("handleReservationNumberIssueFailException", e);
		return new ErrorResponse(e.getMessage(), RESERVATION_NUMBER_ISSUE_FAIL.getCode());
	}

	@ResponseStatus(CONFLICT)
	@ExceptionHandler(ReservationAddUnauthorizedException.class)
	public ErrorResponse handleReservationAddUnauthorizedException(
		ReservationAddUnauthorizedException e, WebRequest request) {
		alarm(e, request);
		log.error("handleReservationAddUnauthorizedException", e);
		return new ErrorResponse(e.getMessage(), RESERVATION_ADD_UNAUTHORIZED.getCode());
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
