package art.aelaort.config;

import art.aelaort.exceptions.CustomTelegramTooManyRequestsException;
import art.aelaort.telegram.client.TelegramClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;

import static art.aelaort.telegram.TelegramBots.telegramUrlSupplier;

@Configuration
public class Config {
	@Value("${proxy-url}")
	private URI proxyUrl;
	@Value("${telegram.api.url}")
	private String telegramUrl;

	private void proxyCustomizer(RestTemplate restTemplate) {
		String host = proxyUrl.getHost();
		int port = proxyUrl.getPort();

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));

		requestFactory.setProxy(proxy);
		restTemplate.setRequestFactory(requestFactory);
	}

	@Bean
	public RestTemplate igRestTemplate(RestTemplateBuilder restTemplateBuilder,
						   @Value("${instagram.api.token}") String igApiToken,
						   @Value("${instagram.api.url}") String igApiUrl) {
		return restTemplateBuilder
				.customizers(this::proxyCustomizer)
				.rootUri(igApiUrl)
				.connectTimeout(Duration.ofSeconds(20))
				.readTimeout(Duration.ofSeconds(20))
				.defaultHeader("x-access-key", igApiToken)
				.build();
	}

	@Bean
	public TelegramClient telegramClient(@Value("${telegram.bot.token}") String token) {
		return TelegramClientBuilder.builder()
				.token(token)
				.telegramUrlSupplier(telegramUrlSupplier(telegramUrl))
				.build();
	}

	@Bean
	public RetryTemplate tgRetryTemplate() {
		return RetryTemplate.builder()
				.maxAttempts(120)
				.fixedBackoff(50)
				.retryOn(TelegramApiRequestException.class)
				.build();
	}

	@Bean
	public RetryTemplate tgApiRequestRetryTemplate() {
		return RetryTemplate.builder()
				.maxAttempts(120)
				.fixedBackoff(50)
				.retryOn(TelegramApiRequestException.class)
				.build();
	}

	@Bean
	public RetryTemplate tgApiTooManyRequestsRetryTemplate() {
		return RetryTemplate.builder()
				.maxAttempts(120)
				.fixedBackoff(50)
				.retryOn(CustomTelegramTooManyRequestsException.class)
				.build();
	}
}
