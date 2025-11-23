package art.aelaort.dto.db;

public record Account(
		int id,
		String account_id,
		String account_nickname,
		String account_title,
		String scan_interval
) {
}
