package com.nowait.discord.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.nowait.discord.config.DiscordFeignConfiguration;
import com.nowait.discord.dto.DiscordMessage;

@FeignClient(
	name = "discordClientUser",
	contextId = "discordClientUser",
	url = "https://discord.com/api/webhooks/1399081071127171203/dvop__zPnQKCX-1VMJyBYnU_KTFNJFUwgGwta9D1Zy0xON0hKDcTW2ke9TQfnywzD2ll",
	configuration = DiscordFeignConfiguration.class)
public interface DiscordClientUser {
	@PostMapping()
	void sendAlarm(@RequestBody DiscordMessage message);
}
