package art.aelaort.service;

import art.aelaort.dto.db.Account;
import art.aelaort.dto.ig.AccountPost;
import art.aelaort.dto.ig.inner.MediaUrl;
import art.aelaort.exceptions.CustomTelegramTooManyRequestsException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TgService {
	private final TelegramClient telegramClient;
	private final IgParserService igParserService;
	private final IgTgMapperService igTgMapperService;
	private final RetryTemplate tgApiRequestRetryTemplate;
	private final RetryTemplate tgApiTooManyRequestsRetryTemplate;
	@Value("${telegram.channel.id}")
	private String telegramChannelId;
	private final Bucket tgApiGroupBucket = Bucket.builder()
			.addLimit(limit -> limit.capacity(20).refillIntervally(20, Duration.ofMinutes(1)))
			.build();
	private final Set<String> tgApiErrors = Set.of(
			"can't parse InputMedia",
			"file must be non-empty",
			"Wrong file identifier/HTTP URL specified"
	);

	public void sendPost(AccountPost post, Account account) {
		List<MediaUrl> mediaUrls = igParserService.parseListMediaUrl(post);
		if (mediaUrls.isEmpty()) {
			log.error("post is empty, account: {}, post: {}", account.accountNickname(), post);
			return;
		}
		send(mediaUrls, account, post);
		log.info("successfully sent, account: {}, post: {}", account.accountNickname(), post);
	}

	private void send(List<MediaUrl> mediaUrls, Account account, AccountPost post) {
		sendMedias(telegramChannelId, mediaUrls, prepareText(post, account));
	}

	private String prepareText(AccountPost post, Account account) {
		return """
				<a href="%s">%s</a>
				
				%s
				
				#%s"""
				.formatted(
						"https://www.instagram.com/p/" + post.urlCode(),
						account.accountTitle(),
						enrichInstagramLinks(post.text()),
						account.accountTitle().split(" ")[0]
				);
	}

	private String enrichInstagramLinks(String srcText) {
		return srcText.replaceAll("@([\\w_.]+)", "<a href=\"https://www.instagram.com/$1\">@$1</a>");
	}

	private void sendMedias(String chatId, List<? extends MediaUrl> mediaUrls, String text) {
		try {
			for (List<? extends MediaUrl> medias : igTgMapperService.partitionIgUrls(mediaUrls)) {
				sendCatchIO(chatId, medias, text);
			}
		} catch (TelegramApiException e) {
			log.error("error sending medias", e);
			throw new RuntimeException(e);
		}
	}

	@Retryable(retryFor = TelegramApiException.class, backoff = @Backoff(delay = 5000), maxAttempts = 20)
	private void sendCatchIO(String chatId, List<? extends MediaUrl> mediaUrls, String text) throws TelegramApiException {
		sendCatchTgTooManyRequests(chatId, mediaUrls, text);
	}

	private void sendCatchTgTooManyRequests(String chatId, List<? extends MediaUrl> mediaUrls, String text) throws TelegramApiException {
		tgApiTooManyRequestsRetryTemplate.execute(context -> {
			if (context.getLastThrowable() != null) {
				if (context.getLastThrowable() instanceof CustomTelegramTooManyRequestsException ee) {
					try {
						log.error("tg api - sleep after 429 - {} seconds", ee.getEx().getParameters().getRetryAfter());
						TimeUnit.SECONDS.sleep(ee.getEx().getParameters().getRetryAfter());
					} catch (InterruptedException e) {
						log.error("tg api - sleep interrupted", e);
						throw new RuntimeException(e);
					}
				}
			}
			sendCatchTgErrors(chatId, mediaUrls, text);
			return null;
		});
	}

	private void sendCatchTgErrors(String chatId, List<? extends MediaUrl> mediaUrls, String text) throws TelegramApiException {
		tgApiRequestRetryTemplate.execute(context -> {
			if (context.getLastThrowable() != null && context.getLastThrowable() instanceof TelegramApiRequestException ee) {
				if (ee.getParameters() != null && ee.getParameters().getRetryAfter() != null) {
					throw new CustomTelegramTooManyRequestsException(ee);
				}
			}
			tgExecuteUrls(chatId, mediaUrls, text);
			return null;
		});
	}

	private void tgExecuteUrls(String chatId, List<? extends MediaUrl> mediaUrls, String text) throws TelegramApiException {
		try {
			List<InputMedia> inputMedias = igTgMapperService.getInputMediaIS(mediaUrls);
			tgExecuteWithRateLimit(chatId, inputMedias, text);
		} catch (TelegramApiRequestException e) {
			boolean knownError = false;
			for (String tgApiError : tgApiErrors) {
				if (e.getApiResponse().contains(tgApiError)) {
					log.error("tg api error: {}, medias: {}", tgApiError, toStringUrls(mediaUrls));
					knownError = true;
					break;
				}
			}
			if (knownError) {
				log.error("error sending, trying another method");
				List<InputMedia> inputMedias = igTgMapperService.getInputMediaLink(mediaUrls);
				tgExecuteWithRateLimit(chatId, inputMedias, text);
			} else {
				throw e;
			}
		}
	}

	private void tgExecuteWithRateLimit(String chatId, List<? extends InputMedia> inputMedias, String text) throws TelegramApiException {
		while (true) {
			ConsumptionProbe consumptionProbe = tgApiGroupBucket.tryConsumeAndReturnRemaining(inputMedias.size() + 1);
			if (consumptionProbe.isConsumed()) {
				tgExecute(chatId, inputMedias, text);
				break;
			} else {
				long waitNanos = consumptionProbe.getNanosToWaitForRefill();
				log.debug("rate limit - sleep for {} sec ({} nanos), consumptionProbe: {}", TimeUnit.NANOSECONDS.toSeconds(waitNanos), waitNanos, consumptionProbe);
				try {
					TimeUnit.NANOSECONDS.sleep(waitNanos);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void tgExecuteWithSleep(String chatId, List<? extends InputMedia> inputMedias, String text) throws TelegramApiException {
		sleep(30);
		tgExecute(chatId, inputMedias, text);
	}

	private void tgExecute(String chatId, List<? extends InputMedia> inputMedias, String text) throws TelegramApiException {
		if (inputMedias.size() > 1) {
			inputMedias.get(0).setCaption(text);
			inputMedias.get(0).setParseMode("html");
			telegramClient.execute(SendMediaGroup.builder()
					.chatId("-100" + chatId)
					.medias(inputMedias)
					.build());
		} else {
			InputMedia inputMedia = inputMedias.get(0);
			if (inputMedia instanceof InputMediaPhoto) {
				telegramClient.execute(SendPhoto.builder()
								.chatId("-100" + chatId)
								.photo(igTgMapperService.toInputFile(inputMedia))
								.caption(text)
								.parseMode("html")
						.build());
			} else if (inputMedia instanceof InputMediaVideo) {
				telegramClient.execute(SendVideo.builder()
						.chatId("-100" + chatId)
						.video(igTgMapperService.toInputFile(inputMedia))
						.caption(text)
						.parseMode("html")
						.build());
			} else {
				throw new IllegalStateException("input media type are not supported");
			}
		}
		log.debug("tg executed successfully with size: {}", inputMedias.size());
	}

	private List<String> toStringUrls(List<? extends MediaUrl> mediaUrls) {
		return mediaUrls.stream().map(MediaUrl::getUrl).toList();
	}

	private void sleep(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
