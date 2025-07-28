package com.nowait.discord.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import com.nowait.discord.client.DiscordClientAdmin;
import com.nowait.discord.dto.DiscordMessage;
import com.nowait.discord.client.DiscordClientUser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordAlarmService {

	private final DiscordClientUser discordClientUser;
	private final DiscordClientAdmin discordClientAdmin;
	private final Environment environment;

	public void sendDiscordUserAlarm(Exception e, WebRequest request) {
		if (shouldNotifyUser()) {
			discordClientUser.sendAlarm(createMessage(e, request));
		}
	}

	public void sendDiscordAdminAlarm(Exception e, WebRequest request) {
		if (shouldNotifyAdmin()) {
			discordClientAdmin.sendAlarm(createMessage(e, request));
		}
	}

	private boolean shouldNotifyAdmin() {
		return Arrays.stream(environment.getActiveProfiles()).noneMatch("admin-local"::equals);
	}

	private boolean shouldNotifyUser() {
		return Arrays.stream(environment.getActiveProfiles()).noneMatch("user-local"::equals);
	}

	private DiscordMessage createMessage(Exception e, WebRequest request) {
		return DiscordMessage.builder()
			.content("# 🚨 에러 발생 비이이이이사아아아앙")
			.embeds(List.of(DiscordMessage.Embed.builder()
				.title("ℹ️ 에러 정보")
				.description(
					"### 🕖 발생 시간\n" + LocalDateTime.now() + "\n" + "### 🔗 요청 URL\n" + createRequestFullPath(request)
					+ "\n" + "### 📄 Stack Trace\n" + "```\n" + getStackTrace(e).substring(0, 1000) + "\n```")
				.build()))
			.build();
	}

	private String createRequestFullPath(WebRequest webRequest) {
		HttpServletRequest request = ((ServletWebRequest)webRequest).getRequest();
		String fullPath = request.getMethod() + " " + request.getRequestURL();
		String queryString = request.getQueryString();
		if (queryString != null) {
			fullPath += "?" + queryString;
		}
		return fullPath;
	}

	private String getStackTrace(Exception e) {
		StringWriter stringWriter = new StringWriter();
		e.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString();
	}
}
