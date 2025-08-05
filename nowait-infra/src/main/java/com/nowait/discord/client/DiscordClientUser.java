package com.nowait.discord.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.nowait.discord.config.DiscordFeignConfiguration;
import com.nowait.discord.dto.DiscordMessage;

@FeignClient(
	name = "discordClientUser",
	contextId = "discordClientUser",
	url = "${discord.webhook.user-url}",
	configuration = DiscordFeignConfiguration.class)
public interface DiscordClientUser {
	@PostMapping()
	void sendAlarm(@RequestBody DiscordMessage message);
}
