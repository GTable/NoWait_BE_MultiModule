package com.nowait.domaincorerdb.user.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nowait.common.enums.Role;
import com.nowait.common.enums.SocialType;
import com.nowait.domaincorerdb.base.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@Getter
@SuperBuilder
public class User extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 생성
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // 카카오 이메일

    @Column(nullable = true)
    private String phoneNumber; // 사용자 전화번호

    @Column(nullable = false)
    private String password; // 관리자 패스워드

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private Boolean isMarketingAgree = false;

    @Column(nullable = false)
    private Boolean phoneEntered = false;

    private Long storeId;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;


    public User(LocalDateTime createdAt, String email,String password, String nickname, String profileImage, SocialType socialType,
        Role role, Long storeId, LocalDateTime updatedAt ) {
        super(createdAt);
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImage = profileImage;
        this.socialType = socialType;
        this.role = role;
        this.storeId = storeId;
        this.updatedAt = updatedAt;
    }

    public static User createUserWithId(Long userId, String email, String nickname, String profileImage,
        SocialType socialType, Role role, Long storeId) {
        User user = User.builder()
            .email(email)
            .nickname(nickname)
            .profileImage(profileImage)
            .socialType(socialType)
            .role(role)
            .storeId(storeId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        user.id = userId;

        return user;
    }

    // User 도메인 관련 비즈니스 로직 (예: 닉네임 변경)
    public void updateNickname(String nickname){
        this.nickname = nickname;
    }

    public void encodePassword(PasswordEncoder passwordEncoder) {
        password = passwordEncoder.encode(password);
    }

    public void setPhoneNumberAndMarkEntered(String phoneNumber, LocalDateTime ts) {
        this.phoneNumber = phoneNumber;
        this.phoneEntered = true;
        this.updatedAt = ts;
    }

    public void setIsMarketingAgree(boolean agree, LocalDateTime ts) {
        this.isMarketingAgree = agree;
        this.updatedAt = ts;
    }
}
