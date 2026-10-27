package com.nowait.applicationadmin.user.dto;

import java.time.LocalDateTime;

import com.nowait.common.enums.Role;
import com.nowait.common.enums.SocialType;
import com.nowait.domaincorerdb.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManagerSignupRequestDto {

	@NotBlank
	@Email(regexp = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
		+ "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$")
	@Schema(description = "이메일(예시)", example = "abc@gmail.com")
	private String email;

	@NotBlank
	@Pattern(regexp = "(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,20}")
	@Schema(description = "비밀번호(예시)", example = "1234568!@")
	private String password;

	@NotBlank
	@Pattern(regexp = "^[a-zA-Z가-힣0-9]{2,12}$", message = "2~12자 사이의 영문, 한글, 숫자만 입력 가능합니다.")
	@Schema(description = "이름(예시)", example = "김노웻")
	private String nickname;

	@Schema(description = "로그인타입", example = "LOCAL")
	private String socialType;

	@Schema(description = "마케팅 수신 동의", example = "true")
	private boolean isMarketingAgree;

	@Schema(description = "폰 번호 입력 여부", example = "true")
	private boolean phoneEntered;

	public User toEntity() {
		return User.builder()
			.profileImage("no")
			.email(email)
			.phoneNumber("")
			.password(password)
			.nickname(nickname)
			.socialType(SocialType.LOCAL)
			.role(Role.MANAGER)
			.isMarketingAgree(isMarketingAgree)
			.phoneEntered(false)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

	}
}
