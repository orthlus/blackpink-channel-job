package art.aelaort.service;

import art.aelaort.dto.ig.inner.MediaUrl;
import art.aelaort.dto.ig.inner.PhotoUrl;
import art.aelaort.dto.ig.inner.VideoUrl;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IgTgMapperService {
	private final IgRequestService igRequestService;

	public List<InputMedia> getInputMediaIS(List<MediaUrl> urls) {
		return urls.stream().map(this::getInputMediaIS).toList();
	}

	public InputMedia getInputMediaIS(MediaUrl url) {
		if (url instanceof VideoUrl) {
			return new InputMediaVideo(igRequestService.download(url), UUID.randomUUID().toString());
		} else if (url instanceof PhotoUrl) {
			return new InputMediaPhoto(igRequestService.download(url), UUID.randomUUID().toString());
		}

		throw new IllegalArgumentException();
	}

	public List<InputMedia> getInputMediaLink(List<MediaUrl> urls) {
		return urls.stream().map(this::getInputMediaLink).toList();
	}

	public InputMedia getInputMediaLink(MediaUrl url) {
		if (url instanceof VideoUrl) {
			return new InputMediaVideo(url.getUrl());
		} else if (url instanceof PhotoUrl) {
			return new InputMediaPhoto(url.getUrl());
		}

		throw new IllegalArgumentException();
	}

	public List<? extends List<? extends InputMedia>> mapIgToTgPosts(List<? extends InputMedia> inputMedias) {
		return Lists.partition(inputMedias, 10);
	}
}
