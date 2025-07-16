package com.nowait.domainadminrdb.statistic.repository;

import java.util.List;

import com.nowait.domainadminrdb.statistic.dto.OrderSalesSumDetail;
import com.nowait.domainadminrdb.statistic.dto.StoreInfo;
import com.nowait.domainadminrdb.statistic.dto.StoreSales;
import com.nowait.domainadminrdb.statistic.dto.TopSalesStoresDetail;



public interface StatisticCustomRepository {

	OrderSalesSumDetail findSalesSumByStoreId(Long storeId);

	List<TopSalesStoresDetail> getTop4PlusMine(Long storeId);


	// redis 사용하는 부분
	List<StoreSales> findTotalSales();

	List<StoreInfo> findStoreInfoByIds(List<Long> storeIds);
}
