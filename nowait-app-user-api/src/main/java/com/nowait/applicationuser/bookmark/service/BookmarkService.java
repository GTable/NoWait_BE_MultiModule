package com.nowait.applicationuser.bookmark.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nowait.applicationuser.bookmark.dto.BookmarkCreateResponse;
import com.nowait.applicationuser.store.dto.StorePageReadResponse;
import com.nowait.applicationuser.store.service.StoreService;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.domaincorerdb.user.entity.User;
import com.nowait.domaincorerdb.user.exception.UserNotFoundException;
import com.nowait.domaincorerdb.user.repository.UserRepository;
import com.nowait.domainuserrdb.bookmark.entity.Bookmark;
import com.nowait.domainuserrdb.bookmark.repository.BookmarkRepository;
import com.nowait.domainuserrdb.oauth.dto.CustomOAuth2User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookmarkService {
	private final BookmarkRepository bookmarkRepository;
	private final StoreRepository storeRepository;
	private final UserRepository userRepository;
	private final StoreService storeService;

	@Transactional
	public BookmarkCreateResponse createBookmark(Long storeId, CustomOAuth2User customOAuth2User) {

		parameterValidation(storeId, customOAuth2User);
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new EntityNotFoundException(storeId + " store not found."));
		User user = userRepository.findById(customOAuth2User.getUserId())
			.orElseThrow(() -> new EntityNotFoundException("User not found"));

		Optional<Bookmark> isBookmark = bookmarkRepository.findRawByUserAndStoreAndDeletedFalse(user, store);

		if (isBookmark.isPresent()) {
			Bookmark bookmark = isBookmark.get();
			if (bookmark.isDeleted()) {
				bookmark.restore();
				return BookmarkCreateResponse.fromEntity(bookmarkRepository.save(bookmark));
			} else {
				throw new IllegalArgumentException("already bookmarked");
			}
		}

		Bookmark bookmark = Bookmark.builder()
			.store(store)
			.user(user)
			.deleted(false)
			.build();

		return BookmarkCreateResponse.fromEntity(bookmarkRepository.save(bookmark));
	}

	@Transactional(readOnly = true)
	public List<StorePageReadResponse> getBookmarks(CustomOAuth2User customOAuth2User) {
		User user = userRepository.findById(customOAuth2User.getUserId())
			.orElseThrow(UserNotFoundException::new);

		List<Long> storeIds = bookmarkRepository.findAllByUserAndDeletedFalse(user)
			.stream()
			.map(Bookmark::getStore)
			.map(Store::getStoreId)
			.toList();

		Set<Long> bookmarkedSet = new HashSet<>(storeIds);

		return storeService.getAllStoresByPageAndDeparments(storeIds, bookmarkedSet);
	}

	@Transactional
	public String deleteBookmark(Long storeId, CustomOAuth2User customOAuth2User) {
		parameterValidation(storeId, customOAuth2User);
		Bookmark bookmark = bookmarkRepository.findActiveByUserIdAndStoreId(storeId, customOAuth2User.getUserId())
			.orElseThrow(() -> new EntityNotFoundException(storeId + " bookmark not found."));
		if (!Objects.equals(bookmark.getUser().getId(), customOAuth2User.getUserId())) {
			throw new IllegalArgumentException("you can only delete your own bookmark");
		}
		bookmark.softDelete();
		return "Bookmark ID " + storeId + " deleted.";
	}

	private static void parameterValidation(Long storeId, CustomOAuth2User customOAuth2User) {
		// 파라미터 유효성 검사
		if (storeId == null || storeId < 0) {
			throw new IllegalArgumentException("storeId must be a positive number");
		}
		if (customOAuth2User == null || customOAuth2User.getUserId() == null) {
			throw new IllegalArgumentException("UserInfo is required");
		}
	}
}
