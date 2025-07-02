package com.nowait.applicationadmin.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Order API", description = "주문 API")
@RestController
@RequestMapping("admin//orders")
@RequiredArgsConstructor
public class OrderController {
	@GetMapping
	public void getOrders() {
		//TODO

	}


}
