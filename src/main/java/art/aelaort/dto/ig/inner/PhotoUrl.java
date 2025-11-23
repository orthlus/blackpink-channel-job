package art.aelaort.dto.ig.inner;

import art.aelaort.dto.ig.AccountPost;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhotoUrl implements MediaUrl {
	private String url;
	private AccountPost accountPost;
}
