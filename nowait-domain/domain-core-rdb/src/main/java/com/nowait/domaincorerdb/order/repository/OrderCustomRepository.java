package com.nowait.domaincorerdb.order.repository;

import java.util.List;

import com.nowait.domaincorerdb.order.dto.OrderSalesSumDetail;
import com.nowait.domaincorerdb.order.dto.TopSalesStoresDetail;

public interface OrderCustomRepository {

	OrderSalesSumDetail findSalesSumByStoreId(Long storeId);

	List<TopSalesStoresDetail> getTop4PlusMine(Long storeId);
}
