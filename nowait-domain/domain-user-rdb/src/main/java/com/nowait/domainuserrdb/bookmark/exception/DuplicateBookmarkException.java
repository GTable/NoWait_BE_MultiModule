package com.nowait.domainuserrdb.bookmark.exception;

import com.nowait.common.exception.ErrorMessage;

public class DuplicateBookmarkException extends RuntimeException {
	public DuplicateBookmarkException() {
		super(ErrorMessage.DUPLICATE_BOOKMARK.getMessage());
	}
}
