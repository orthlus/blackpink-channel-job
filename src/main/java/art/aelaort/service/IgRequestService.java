package art.aelaort.service;

import art.aelaort.dto.db.Account;
import art.aelaort.dto.ig.AccountPost;
import art.aelaort.dto.ig.inner.MediaUrl;
import art.aelaort.exceptions.IgRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class IgRequestService {
	private final RestTemplate igRestTemplate;
	private final ObjectMapper objectMapper;

	public List<AccountPost> getPostsByAccount(Account account) {
		return requestPosts("/v1/user/medias/chunk?user_id=" + account.accountId());
	}

	public List<AccountPost> getPostsByAccount(Account account, String endCursor) {
		return requestPosts("/v1/user/medias/chunk?user_id=%s&end_cursor=%s".formatted(account.accountId(), endCursor));
	}

	@Retryable(retryFor = IgRequestException.class, backoff = @Backoff(delay = 10000), maxAttempts = 5)
	private List<AccountPost> requestPosts(String path) {
		try {
			String responseStr = igRestTemplate.getForObject(path, String.class);
			List<Object> result = objectMapper.readValue(responseStr, listObjectTypeReference());
			AccountPost[] accountPosts = objectMapper.convertValue(result.get(0), AccountPost[].class);

			return Arrays.asList(requireNonNull(accountPosts));
		} catch (IllegalStateException | RestClientException e) {
			throw new IgRequestException(e);
		} catch (JsonProcessingException e) {
			log.error("Failed to parse response from ig", e);
			return List.of();
		}
	}

	private static TypeReference<List<Object>> listObjectTypeReference() {
		return new TypeReference<>() {
		};
	}

	@Retryable(backoff = @Backoff(delay = 10000))
	public InputStream download(MediaUrl url) {
		byte[] bytes = igRestTemplate.getForObject(URI.create(url.getUrl()), byte[].class);
		checkBytes(bytes, url.getUrl());
		return new ByteArrayInputStream(requireNonNull(bytes));
	}

	private void checkBytes(byte[] bytes, String url) {
		if (bytes.length < 1) {
			log.error("ig download - bytes.length < 1, url: {}", url);
		}
		if (bytes.length < 100) {
			log.error("ig download - bytes.length < 100, url: {}", url);
		}
		if (bytes.length < 1000) {
			log.error("ig download - bytes.length < 1000, url: {}", url);
		}
		if (bytes.length < 10000) {
			log.error("ig download - bytes.length < 10000, url: {}", url);
		}
	}
}
