package art.aelaort.dto.ig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountPost(
		String pk,
		String code,
		@JsonProperty("media_type") // 1, 2, 8
		int mediaType,
		@JsonProperty("thumbnail_url")
		String singlePhotoUrl,
		@JsonProperty("video_url")
		String singleVideoUrl,
		@JsonProperty("resources") // media_type = 8
		List<AccountPost> posts
) {
}
