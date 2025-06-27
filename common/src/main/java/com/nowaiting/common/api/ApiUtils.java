// common/api/ApiUtils.java
package com.nowaiting.common.api;

import com.nowaiting.common.enums.HttpStatusCode;

public class ApiUtils {
	public static <T> ApiResult<T> success(T response) {
		return new ApiResult<>(true, response, null);
	}

	public static ApiResult<?> error(Throwable throwable, HttpStatusCode status) {
		return new ApiResult<>(false, null, new ApiError(throwable.getMessage(), status.value()));
	}

	public static ApiResult<?> error(String message, HttpStatusCode status) {
		return new ApiResult<>(false, null, new ApiError(message, status.value()));
	}
}
