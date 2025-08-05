package com.nowait.discord.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.nowait.discord.config.DiscordFeignConfiguration;
import com.nowait.discord.dto.DiscordMessage;

@FeignClient(
	name = "discordClientAdmin",
	contextId = "discordClientAdmin",
	url = "${discord.webhook.admin-url}",
	configuration = DiscordFeignConfiguration.class)
public interface DiscordClientAdmin {
	@PostMapping()
	void sendAlarm(@RequestBody DiscordMessage message);
}
