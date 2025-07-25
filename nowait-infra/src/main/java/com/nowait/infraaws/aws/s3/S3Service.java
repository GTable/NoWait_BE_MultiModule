package com.nowait.infraaws.aws.s3;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class S3Service {
	private final AmazonS3Client amazonS3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String originalBucket;

	@Value("${cloud.aws.s3.resize-bucket}")
	private String resizeBucket;

	public record S3UploadResult(String key, String originalUrl, String resizedUrl) {
	}

	@Bulkhead(name = "s3UploadBulkhead", type = Bulkhead.Type.THREADPOOL)
	@Async("s3UploadExecutor")
	public CompletableFuture<S3UploadResult> upload(String type, Long refId,
		MultipartFile file) {  // TODO MultipartFile 분리 필요 (Spring에 의존하면 안 됨)
		try (InputStream inputStream = file.getInputStream()) {
			String key = createFileKey(type, refId, file.getOriginalFilename());
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(file.getSize());

			// 1) 원본 버킷에 이미지 업로드
			amazonS3Client.putObject(originalBucket, key, inputStream, metadata);

			// 2) 각 버킷의 URL 생성
			String originalUrl = amazonS3Client.getUrl(originalBucket, key).toString();
			String resizedUrl = amazonS3Client.getUrl(resizeBucket, key).toString();

			return CompletableFuture.completedFuture(new S3UploadResult(key, originalUrl, resizedUrl));
		} catch (Exception e) {
			throw new RuntimeException("S3 업로드 실패", e);
		}
	}

	public void delete(String key) {
		try {
			amazonS3Client.deleteObject(originalBucket, key);
			amazonS3Client.deleteObject(resizeBucket,  key);
		} catch (Exception e) {
			throw new RuntimeException("S3 파일 삭제 실패", e);
		}
	}

	private String createFileKey(String type, Long refId, String filename) {
		return type + "/" + refId + "/" + UUID.randomUUID() + "-" + filename;
	}
}
