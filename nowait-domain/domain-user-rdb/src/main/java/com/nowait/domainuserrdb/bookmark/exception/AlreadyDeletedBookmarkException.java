package com.nowait.domainuserrdb.bookmark.exception;

import com.nowait.common.exception.ErrorMessage;

public class AlreadyDeletedBookmarkException extends RuntimeException {
	public AlreadyDeletedBookmarkException() { super(ErrorMessage.ALREADY_DELETED_BOOKMARK.getMessage()); }
}
