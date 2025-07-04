package com.nowait.domaincorerdb.menu.entity;

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
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class Menu extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long Id;

	@Column(nullable = false)
	private Long storeId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = true)
	private String description;

	@Column(nullable = true)
	private Integer price;

	@Column(nullable = false)
	private	Boolean isSoldOut;

	@Column(nullable = false)
	private Boolean deleted;


	public Menu(LocalDateTime createdAt, Long id, Long storeId, String name, String description, Integer price, Boolean isSoldOut, Boolean deleted) {
		super(createdAt);
		this.Id = id;
		this.storeId = storeId;
		this.name = name;
		this.description = description;
		this.price = price;
		this.isSoldOut = isSoldOut != null ? isSoldOut : false;
		this.deleted = deleted != null ? deleted : false;
	}

	public void updateInfo(String name, String description, Integer price) {
		if (name != null) this.name = name;
		if (description != null) this.description = description;
		if (price != null) this.price = price;
	}

	public void markAsDeleted() { this.deleted = true; }

	public void toggleSoldOut() { this.isSoldOut = !this.isSoldOut; }
}
