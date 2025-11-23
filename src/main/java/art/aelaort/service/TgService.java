package art.aelaort.service;

import art.aelaort.dto.db.Account;
import art.aelaort.dto.ig.AccountPost;
import art.aelaort.dto.ig.inner.MediaUrl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static art.aelaort.telegram.client.TelegramClientHelpers.execute;

@Slf4j
@Component
@RequiredArgsConstructor
public class TgService {
	private final TelegramClient telegramClient;
	private final IgParserService igParserService;
	private final IgTgMapperService igTgMapperService;

	@Value("${telegram.channel.id}")
	private String telegramChannelId;

	public void sendPost(AccountPost post, Account account) {
		List<MediaUrl> mediaUrls = igParserService.parseListMediaUrl(post);
		if (mediaUrls.isEmpty()) {
			log.error("post is empty, account: {}, post: {}", account, post);
			return;
		}
		send(mediaUrls, account, post);
	}

	private void send(List<MediaUrl> mediaUrls, Account account, AccountPost post) {
		try {
			sendByLinks(mediaUrls, account, post);
		} catch (Exception e) {
			log.error("error sending links, trying send files", e);
			sendByBytes(mediaUrls, account, post);
		}
	}

	private void sendByLinks(List<MediaUrl> mediaUrls, Account account, AccountPost post) {
		List<InputMedia> list = igTgMapperService.getInputMediaLink(mediaUrls);
		sendMedias(telegramChannelId, list, prepareText(post, account));
	}

	private void sendByBytes(List<MediaUrl> mediaUrls, Account account, AccountPost post) {
		List<InputMedia> list = igTgMapperService.getInputMediaIS(mediaUrls);
		sendMedias(telegramChannelId, list, prepareText(post, account));
	}

	private String prepareText(AccountPost post, Account account) {
		return """
				[%s](%s)
				
				%s
				
				#%s"""
				.formatted(
						account.accountTitle(),
						"https://www.instagram.com/p/" + post.urlCode(),
						post.text(),
						account.accountTitle().split(" ")[0]
				);
	}

	private void sendMedias(String chatId, List<? extends InputMedia> inputMedias, String text) {
		for (List<? extends InputMedia> medias : igTgMapperService.mapIgToTgPosts(inputMedias)) {
			medias.get(0).setCaption(text);
			medias.get(0).setParseMode("markdown");
			executeSendMediaGroup(chatId, medias);
		}
	}

	@Retryable(retryFor = TelegramApiException.class, backoff = @Backoff(delay = 10000), maxAttempts = 20)
	private void executeSendMediaGroup(String chatId, List<? extends InputMedia> inputMedias) {
		execute(SendMediaGroup.builder()
						.chatId(chatId)
						.medias(inputMedias),
				telegramClient);
	}
}
