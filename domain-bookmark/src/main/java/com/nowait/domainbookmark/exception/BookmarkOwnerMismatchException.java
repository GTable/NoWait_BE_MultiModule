package com.nowait.domainbookmark.exception;

import com.nowaiting.common.exception.ErrorMessage;

public class BookmarkOwnerMismatchException extends RuntimeException {
	public BookmarkOwnerMismatchException() {
		super(ErrorMessage.NOT_OWN_BOOKMARK.getMessage());
	}
}
