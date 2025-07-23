package com.nowait.domaincorerdb.store.repository;

import org.springframework.stereotype.Repository;

import com.nowait.domaincorerdb.store.entity.QStore;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StoreCustomRepositoryImpl implements StoreCustomRepository {

	private final JPAQueryFactory queryFactory;

	private final QStore store = QStore.store;
}
