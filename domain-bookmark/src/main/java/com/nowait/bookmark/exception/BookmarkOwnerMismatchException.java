package com.nowait.bookmark.exception;

import com.nowait.common.exception.ErrorMessage;

public class BookmarkOwnerMismatchException extends RuntimeException {
	public BookmarkOwnerMismatchException() {
		super(ErrorMessage.NOT_OWN_BOOKMARK.getMessage());
	}
}
