package com.nowait.discord.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.nowait.discord.config.DiscordFeignConfiguration;
import com.nowait.discord.dto.DiscordMessage;

@FeignClient(
	name        = "discordClientAdmin",
	contextId   = "discordClientAdmin",
	url = "https://discord.com/api/webhooks/1399070629994565653/Cy1_421v3-rN7U_7mKO7tcbwnGxS6Ufmf-T-uorAp8Bn0EoqDlrnKKvaf91PftxD1fRi",
	configuration = DiscordFeignConfiguration.class)
public interface DiscordClientAdmin {
	@PostMapping()
	void sendAlarm(@RequestBody DiscordMessage message);
}
