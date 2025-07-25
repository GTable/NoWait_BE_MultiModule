package com.nowait.domaincorerdb.store.entity;

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
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class Store extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long storeId;

	@Column(name = "department_id", nullable = false)
	private Long departmentId;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = true, length = 200)
	private String location;

	@Column(nullable = true, length = 200)
	private String description;

	@Column(nullable = true, length = 200)
	private String notice;

	@Column(nullable = true, length = 200)
	private String openTime;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(nullable = false)
	private Boolean deleted;

	public Store(LocalDateTime createdAt, Long storeId, Long departmentId, String name, String location,
		String description,String notice,String openTime, Boolean isActive, Boolean deleted) {
		super(createdAt);
		this.storeId = storeId;
		this.departmentId = departmentId;
		this.name = name;
		this.location = location;
		this.description = description;
		this.notice = notice;
		this.openTime = openTime;
		this.isActive = isActive;
		this.deleted = deleted;
	}

	public void updateInfo(String name, String location, String description, String notice, String openTime) {
		if (name != null) this.name = name;
		if (location != null) this.location = location;
		if (description != null) this.description = description;
		if (notice != null) this.notice = notice;
		if (openTime != null) this.openTime = openTime;
	}

	public void markAsDeleted() {
		this.deleted = true;
	}

	public void toggleActive() {
		this.isActive = !this.isActive;
	}

}
