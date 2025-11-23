package art.aelaort.service;

import art.aelaort.dto.db.Account;
import art.aelaort.dto.ig.AccountPost;
import art.aelaort.dto.ig.inner.MediaUrl;
import art.aelaort.exceptions.IgRequestException;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class IgRequestService {
	private final RestTemplate igRestTemplate;

	public List<AccountPost> getPostsByAccount(Account account) {
		return requestPosts("/v1/user/medias/chunk?user_id=" + account.accountId());
	}

	public List<AccountPost> getPostsByAccount(Account account, String endCursor) {
		return requestPosts("/v1/user/medias/chunk?user_id=%s&end_cursor=%s".formatted(account.accountId(), endCursor));
	}

	@Retryable(retryFor = IgRequestException.class, backoff = @Backoff(delay = 10000), maxAttempts = 5)
	private List<AccountPost> requestPosts(String path) {
		try {
			AccountPost[] accountPosts = igRestTemplate.getForObject(path, AccountPost[].class);
			return Arrays.asList(requireNonNull(accountPosts));
		} catch (IllegalStateException | RestClientException e) {
			throw new IgRequestException(e);
		}
	}

	@Retryable(backoff = @Backoff(delay = 10000))
	public InputStream download(MediaUrl url) {
		byte[] bytes = igRestTemplate.getForObject(URI.create(url.getUrl()), byte[].class);
		return new ByteArrayInputStream(requireNonNull(bytes));
	}
}
