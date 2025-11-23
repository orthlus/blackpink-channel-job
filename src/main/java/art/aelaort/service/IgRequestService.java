package art.aelaort.service;

import art.aelaort.dto.ig.AccountPost;
import art.aelaort.dto.ig.inner.MediaUrl;
import art.aelaort.exceptions.IgRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class IgRequestService {
	private final RestTemplate igRestTemplate;
	private final IgParserService iGParserService;

	@Retryable
	private List<MediaUrl> requestMediaUrl(String path) {
		try {
			AccountPost accountPost = igRestTemplate.getForObject(path, AccountPost.class);
			return iGParserService.parseListMediaUrl(requireNonNull(accountPost));
		} catch (IllegalStateException | RestClientException e) {
			throw new IgRequestException(e);
		}
	}

	@Retryable
	public InputStream download(MediaUrl url) {
		byte[] bytes = igRestTemplate.getForObject(URI.create(url.getUrl()), byte[].class);
		return new ByteArrayInputStream(requireNonNull(bytes));
	}
}
