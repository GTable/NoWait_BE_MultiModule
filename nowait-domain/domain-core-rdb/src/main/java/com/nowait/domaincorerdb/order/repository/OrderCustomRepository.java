package com.nowait.domaincorerdb.order.repository;

import com.nowait.domaincorerdb.order.dto.OrderSalesSumDetail;

public interface OrderCustomRepository {

	OrderSalesSumDetail findSalesSumByStoreId(Long storeId);
}
