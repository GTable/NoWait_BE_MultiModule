package com.nowait.applicationadmin.exception;

import static com.nowait.common.exception.ErrorMessage.*;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.*;

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
import org.springframework.web.multipart.MultipartException;

import com.nowait.applicationadmin.security.exception.ResourceNotFoundException;
import com.nowait.applicationadmin.security.exception.UnauthorizedException;
import com.nowait.common.exception.ErrorMessage;
import com.nowait.common.exception.ErrorResponse;
import com.nowait.domaincorerdb.order.exception.DuplicateOrderException;
import com.nowait.domaincorerdb.order.exception.OrderItemsEmptyException;
import com.nowait.domaincorerdb.order.exception.OrderParameterEmptyException;
import com.nowait.domaincorerdb.reservation.exception.ReservationNotFoundException;
import com.nowait.domaincorerdb.token.exception.BusinessException;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(BusinessException.class)
	public ErrorResponse handleBusinessException(BusinessException e) {
		log.error("handleBusinessException", e);
		return new ErrorResponse(e.getMessage(), e.getCode());
	}

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		log.error("handleMethodArgumentNotValidException", e);
		Map<String, String> errors = getErrors(e);
		return new ErrorResponse(INVALID_INPUT_VALUE.getMessage(), INVALID_INPUT_VALUE.getCode(), errors);
	}

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		log.error("handleHttpMessageNotReadableException", e);
		return new ErrorResponse(INVALID_INPUT_VALUE.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(IllegalArgumentException.class)
	public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e) {
		log.error("handleIllegalArgumentException", e);
		return new ErrorResponse(e.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(MissingRequestValueException.class)
	public ErrorResponse handleMissingRequestValueException(MissingRequestValueException e) {
		log.error("handleMissingRequestValueExceptionException", e);
		return new ErrorResponse(INVALID_INPUT_VALUE.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(value = UNAUTHORIZED)
	@ExceptionHandler(UnauthorizedException.class)
	public ErrorResponse handleUnauthorizedException(UnauthorizedException e) {
		log.error("handleUnauthorizedExceptionException", e);
		return new ErrorResponse(e.getMessage(), e.getCode());
	}

	@ResponseStatus(value = NOT_FOUND)
	@ExceptionHandler(ResourceNotFoundException.class)
	public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException e) {
		log.error("handleResourceNotFoundExceptionException", e);
		return new ErrorResponse(e.getMessage(), e.getCode());
	}

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(MultipartException.class)
	public ErrorResponse handleMultipartException(MultipartException e) {
		log.error("handleMultipartException", e);
		return new ErrorResponse(e.getMessage(), INVALID_INPUT_VALUE.getCode());
	}

	@ResponseStatus(value = NOT_FOUND)
	@ExceptionHandler(UserNotFoundException.class)
	public ErrorResponse userNotFoundException(UserNotFoundException e) {
		log.error("userNotFoundException", e);
		return new ErrorResponse(e.getMessage(), NOTFOUND_USER.getCode());
	}

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(OrderParameterEmptyException.class)
	public ErrorResponse orderParameterEmptyException(OrderParameterEmptyException e) {
		log.error("orderParameterEmptyException", e);
		return new ErrorResponse(e.getMessage(), ORDER_PARAMETER_EMPTY.getCode());
	}

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(OrderItemsEmptyException.class)
	public ErrorResponse orderItemsEmptyException(OrderItemsEmptyException e) {
		log.error("orderItemsEmptyException", e);
		return new ErrorResponse(e.getMessage(), ORDER_ITEMS_EMPTY.getCode());
	}

	@ResponseStatus(value = BAD_REQUEST)
	@ExceptionHandler(DuplicateOrderException.class)
	public ErrorResponse duplicateOrderException(DuplicateOrderException e) {
		log.error("duplicateOrderException", e);
		return new ErrorResponse(e.getMessage(), ErrorMessage.DUPLICATE_ORDER.getCode());
	}

	@ResponseStatus(value = NOT_FOUND)
	@ExceptionHandler(ReservationNotFoundException.class)
	public ErrorResponse reservationNotFoundException(ReservationNotFoundException e) {
		log.error("reservationNotFoundException", e);
		return new ErrorResponse(e.getMessage(), NOTFOUND_RESERVATION.getCode());
	}


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
