package com.nowait.domaincorerdb.department.exception;

import com.nowait.common.exception.ErrorMessage;

public class DepartmentNotFoundException extends RuntimeException {
	public DepartmentNotFoundException() {
		super(ErrorMessage.DEPARTMENT_NOT_FOUND.getMessage());
	}
}
