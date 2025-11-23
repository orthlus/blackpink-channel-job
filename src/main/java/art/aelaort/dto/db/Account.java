package art.aelaort.dto.db;

public record Account(
		int id,
		String accountId,
		String accountNickname,
		String accountTitle,
		String scanInterval,
		boolean fetchStories
) {
}
