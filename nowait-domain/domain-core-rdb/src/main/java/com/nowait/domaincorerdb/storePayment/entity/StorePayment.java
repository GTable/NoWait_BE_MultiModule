package com.nowait.domaincorerdb.storepayment.entity;

import java.time.LocalDateTime;

import com.nowait.domaincorerdb.base.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "store_payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class StorePayment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long paymentMethodId;

	@Column(name = "store_id", nullable = false)
	private Long storeId;

	@Column(name = "toss_url",length = 500)
	private String tossUrl;

	@Column(name = "kakao_pay_url", nullable = true, length = 500)
	private String kakaoPayUrl;

	@Column(name = "naver_pay_url", nullable = true, length = 500)
	private String naverPayUrl;

	@Column(name = "account_number", nullable = true, length = 45)
	private String accountNumber;

	public StorePayment(LocalDateTime createdAt, Long paymentMethodId, Long storeId, String tossUrl, String kakaoPayUrl, String naverPayUrl, String accountNumber) {
		super(createdAt);
		this.paymentMethodId = paymentMethodId;
		this.storeId = storeId;
		this.tossUrl = tossUrl;
		this.kakaoPayUrl = kakaoPayUrl;
		this.naverPayUrl = naverPayUrl;
		this.accountNumber = accountNumber;
	}

	public void updatePaymentMethodUrl(String tossUrl, String kakaoPayUrl, String naverPayUrl, String accountNumber) {
		if (tossUrl != null) this.tossUrl = tossUrl;
		if (kakaoPayUrl != null) this.kakaoPayUrl = kakaoPayUrl;
		if (naverPayUrl != null) this.naverPayUrl = naverPayUrl;
		if (accountNumber != null) this.accountNumber = accountNumber;
	}
}

