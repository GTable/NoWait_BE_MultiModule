package com.nowait.applicationadmin.menu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nowait.applicationadmin.menu.dto.MenuImageUploadResponse;
import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.entity.MenuImage;
import com.nowait.domaincorerdb.menu.exception.MenuImageEmptyException;
import com.nowait.domaincorerdb.menu.exception.MenuImageNotFoundException;
import com.nowait.domaincorerdb.menu.exception.MenuNotFoundException;
import com.nowait.domaincorerdb.menu.repository.MenuImageRepository;
import com.nowait.domaincorerdb.menu.repository.MenuRepository;
import com.nowait.infraaws.aws.s3.S3Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuImageService {

	private final MenuRepository menuRepository;
	private final MenuImageRepository menuImageRepository;
	private final S3Service s3Service;

	@Transactional
	public MenuImageUploadResponse save(Long menuId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new MenuImageEmptyException();
		}

		String type = "menu";
		Menu menu = menuRepository.findById(menuId)
			.orElseThrow(MenuNotFoundException::new);

		S3Service.S3UploadResult uploadResult = s3Service.upload(type, menuId, file).join();

		// MenuImage 엔티티 생성 및 저장
		MenuImage menuImage = MenuImage.builder()
			.menu(menu)
			.imageUrl(uploadResult.url())
			.fileKey(uploadResult.key())
			.build();

		menuImageRepository.save(menuImage);

		// 응답 생성
		return MenuImageUploadResponse.fromEntity(menuImage);
	}

	@Transactional
	public void delete(Long id) {
		MenuImage menuImage = menuImageRepository.findById(id)
			.orElseThrow(MenuImageNotFoundException::new);

		s3Service.delete(menuImage.getFileKey());
		menuImageRepository.delete(menuImage);
	}
}
