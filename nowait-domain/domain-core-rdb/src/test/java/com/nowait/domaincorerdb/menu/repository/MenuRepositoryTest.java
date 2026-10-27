package com.nowait.domaincorerdb.menu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.nowait.domaincorerdb.menu.entity.Menu;
import com.nowait.domaincorerdb.menu.entity.MenuImage;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class MenuRepositoryTest {

	@SpringBootConfiguration
	@EntityScan(basePackageClasses = Menu.class)
	@EnableJpaRepositories(basePackageClasses = MenuRepository.class)
	static class Config {
	}

	@Autowired
	MenuRepository menuRepository;

	@Autowired
	TestEntityManager em;

	@Test
	void 메뉴와_이미지를_쿼리_한번에_메뉴별로_묶는다() {
		Menu second = menu(1L, 2L, false);
		Menu first = menu(1L, 1L, false);
		Menu noImage = menu(1L, 3L, false);
		menu(1L, 4L, true);
		menu(2L, 1L, false);
		MenuImage a = image(first);
		MenuImage b = image(second);
		MenuImage c = image(second);
		em.flush();
		em.clear();
		Statistics stats = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		stats.clear();

		Map<Menu, List<MenuImage>> result = menuRepository.findAllWithImagesByStoreId(1L);

		assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
		// sortOrder 순서, 삭제된 메뉴·다른 주점 제외, 이미지 없는 메뉴는 빈 목록
		assertThat(result.keySet()).extracting(Menu::getId)
			.containsExactly(first.getId(), second.getId(), noImage.getId());
		assertThat(result.values()).extracting(images -> images.stream().map(MenuImage::getId).toList())
			.containsExactly(List.of(a.getId()), List.of(b.getId(), c.getId()), List.of());
	}

	private Menu menu(Long storeId, Long sortOrder, boolean deleted) {
		return em.persist(Menu.builder().storeId(storeId).name("menu").isSoldOut(false).sortOrder(sortOrder)
			.deleted(deleted).build());
	}

	private MenuImage image(Menu menu) {
		return em.persist(MenuImage.builder().menu(menu).imageUrl("url").fileKey("key").build());
	}
}
