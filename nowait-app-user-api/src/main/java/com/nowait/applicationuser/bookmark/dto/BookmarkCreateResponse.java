package com.nowait.applicationuser.bookmark.dto;

import com.nowait.domainuserrdb.bookmark.entity.Bookmark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class BookmarkCreateResponse {
	private Long bookmarkId;
	private Long userId;
	private Long storeId;

	public static BookmarkCreateResponse fromEntity(Bookmark bookmark) {
		return BookmarkCreateResponse.builder()
			.bookmarkId(bookmark.getId())
			.userId(bookmark.getUser().getId())
			.storeId(bookmark.getStore().getStoreId())
			.build();
	}
}
