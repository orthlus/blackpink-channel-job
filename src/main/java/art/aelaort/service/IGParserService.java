package art.aelaort.service;

import art.aelaort.dto.ig.AccountPost;
import art.aelaort.dto.ig.inner.MediaUrl;
import art.aelaort.dto.ig.inner.PhotoUrl;
import art.aelaort.dto.ig.inner.VideoUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class IGParserService {
	public List<MediaUrl> parseListMediaUrl(AccountPost accountPost) {
		if (accountPost.mediaType() == 8) {
			return accountPost.posts().stream().map(this::parseSingleMediaUrl).toList();
		}
		return List.of(parseSingleMediaUrl(accountPost));
	}

	private MediaUrl parseSingleMediaUrl(AccountPost accountPost) {
		return switch (accountPost.mediaType()) {
			case 1 -> new PhotoUrl(accountPost.singlePhotoUrl());
			case 2 -> new VideoUrl(accountPost.singleVideoUrl());
			default -> throw new IllegalStateException("Unexpected value: " + accountPost.mediaType());
		};
	}
}
