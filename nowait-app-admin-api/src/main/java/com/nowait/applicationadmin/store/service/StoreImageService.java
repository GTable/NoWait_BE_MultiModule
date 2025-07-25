package com.nowait.applicationadmin.store.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nowait.applicationadmin.store.dto.StoreImageUploadResponse;

import com.nowait.domaincorerdb.store.entity.ImageType;
import com.nowait.domaincorerdb.store.entity.Store;
import com.nowait.domaincorerdb.store.entity.StoreImage;
import com.nowait.domaincorerdb.store.exception.StoreImageEmptyException;
import com.nowait.domaincorerdb.store.exception.StoreImageNotFoundException;
import com.nowait.domaincorerdb.store.exception.StoreNotFoundException;
import com.nowait.domaincorerdb.store.repository.StoreImageRepository;
import com.nowait.domaincorerdb.store.repository.StoreRepository;
import com.nowait.infraaws.aws.s3.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreImageService {

	private final StoreRepository storeRepository;
	private final StoreImageRepository storeImageRepository;
	private final S3Service s3Service;

	@Transactional
	public List<StoreImageUploadResponse> saveAll(Long storeId, List<MultipartFile> files) {
		if (files == null || files.isEmpty())
			throw new StoreImageEmptyException();

		String type = "banner";
		Store store = storeRepository.findById(storeId)
			.orElseThrow(StoreNotFoundException::new);

		// 모든 파일을 비동기로 업로드
		List<CompletableFuture<S3Service.S3UploadResult>> uploadFutures = new ArrayList<>();
		for (MultipartFile file : files) {
			uploadFutures.add(s3Service.upload(type, storeId, file));
		}

		// 모든 업로드 완료 대기
		List<S3Service.S3UploadResult> uploadResults;
		try {
			uploadResults = uploadFutures.stream()
				.map(CompletableFuture::join)
				.toList();
		} catch (Exception e) {
			throw new RuntimeException("S3 업로드 실패", e);
		}

		// DB 저장은 모든 S3 업로드 성공 후 수행
		List<StoreImageUploadResponse> imageUploadResponses = new ArrayList<>();
		for (S3Service.S3UploadResult uploadResult : uploadResults) {
			StoreImage storeImage = StoreImage.builder()
				.store(store)
				.imageUrl(uploadResult.resizedUrl())
				.fileKey(uploadResult.key())
				.imageType(ImageType.BANNER)
				.build();

			storeImageRepository.save(storeImage);
			imageUploadResponses.add(StoreImageUploadResponse.fromEntity(storeImage));
		}

		return imageUploadResponses;
	}

	@Transactional
	public StoreImageUploadResponse saveProfileImage(Long storeId, MultipartFile file) {

		String type = "profile";
		Store store = storeRepository.findById(storeId)
			.orElseThrow(StoreNotFoundException::new);

		Optional<StoreImage> existingProfileImage = storeImageRepository.findByStoreStoreIdAndImageType(store.getStoreId(),
			ImageType.PROFILE);

		existingProfileImage.ifPresent(profile -> {
			s3Service.delete(profile.getFileKey());
			storeImageRepository.delete(profile);
		});

		S3Service.S3UploadResult uploadResult = s3Service.upload(type, storeId, file).join();

		// StoreImage 엔티티 생성 및 저장
		StoreImage storeImage = StoreImage.builder()
			.store(store)
			.imageUrl(uploadResult.resizedUrl())
			.fileKey(uploadResult.key())
			.imageType(ImageType.PROFILE)
			.build();

		storeImageRepository.save(storeImage);

		return StoreImageUploadResponse.fromEntity(storeImage);
	}

	@Transactional
	public void delete(Long storeImageId) {
		StoreImage storeImage = storeImageRepository.findById(storeImageId)
			.orElseThrow(StoreImageNotFoundException::new);

		s3Service.delete(storeImage.getFileKey());
		storeImageRepository.delete(storeImage);
	}
}
