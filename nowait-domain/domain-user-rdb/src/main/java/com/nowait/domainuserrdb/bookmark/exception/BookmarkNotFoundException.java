package com.nowait.domainuserrdb.bookmark.exception;

import com.nowait.common.exception.ErrorMessage;

public class BookmarkNotFoundException extends RuntimeException {
	public BookmarkNotFoundException() {
		super(ErrorMessage.NOT_FOUND_BOOKMARK.getMessage());
	}
}
