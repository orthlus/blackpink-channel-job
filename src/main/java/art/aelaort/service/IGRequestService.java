package art.aelaort.service;

import art.aelaort.dto.ig.AccountPost;
import art.aelaort.dto.ig.inner.MediaUrl;
import art.aelaort.exceptions.IgRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class IGRequestService {
	private final RestTemplate igRestTemplate;
	private final IGParserService iGParserService;

	@Retryable
	private List<MediaUrl> requestMediaUrl(String path) {
		try {
			AccountPost accountPost = igRestTemplate.getForObject(path, AccountPost.class);
			return iGParserService.parseListMediaUrl(requireNonNull(accountPost));
		} catch (IllegalStateException | RestClientException e) {
			throw new IgRequestException(e);
		}
	}
}
