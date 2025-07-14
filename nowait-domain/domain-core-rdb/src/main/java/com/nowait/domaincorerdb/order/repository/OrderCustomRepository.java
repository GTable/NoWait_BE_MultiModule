package com.nowait.domaincorerdb.order.repository;

import com.nowait.domaincorerdb.order.dto.OrderSalesSumResponse;

public interface OrderCustomRepository {

	OrderSalesSumResponse findSalesSumByStoreId(Long storeId);
}
