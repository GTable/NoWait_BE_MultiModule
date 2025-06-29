package com.example.menu.entity;

import java.time.LocalDateTime;
import com.nowait.base.BaseTimeEntity;

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
	private Long storeId;
	private String name;
	private String description;
	private Integer price;
	private	Boolean isSoldOut;
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
