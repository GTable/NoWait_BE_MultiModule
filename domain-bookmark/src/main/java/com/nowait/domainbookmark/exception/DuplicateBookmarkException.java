package com.nowait.domainbookmark.exception;

import com.nowaiting.common.exception.ErrorMessage;

public class DuplicateBookmarkException extends RuntimeException {
	public DuplicateBookmarkException() {
		super(ErrorMessage.DUPLICATE_BOOKMARK.getMessage());
	}
}
