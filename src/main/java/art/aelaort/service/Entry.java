package art.aelaort.service;

import art.aelaort.dto.db.Account;
import art.aelaort.dto.ig.AccountPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class Entry implements CommandLineRunner {
	private final Repo repo;
	private final IgApiService igApiService;
	private final TgService tgService;
	@Value("${scan.interval}")
	private String scanInterval;

	@Override
	public void run(String... args) throws Exception {
		Set<Account> accounts = repo.getAccountsByInterval(scanInterval);
		log.info("start with interval: {}, accounts: {}", scanInterval, accounts.size());
		accounts.stream().filter(Account::fetchStories).forEach(account -> log.warn("fetching stories not implemented (account: {})", account.accountId()));

		for (Account account : accounts) {
			log.info("start handling account: {}", account.accountNickname());
			List<AccountPost> newPostsByAccount = igApiService.getNewPostsByAccount(account, 2);
			newPostsByAccount.stream()
					.sorted(Comparator.comparing(AccountPost::id))
					.forEach(post -> tgService.sendPost(post, account));
			if (newPostsByAccount.isEmpty()) {
				log.info("account: {} - 0 new posts", account.accountNickname());
				continue;
			}
			log.info("account: {} - sent {} posts", account.accountNickname(), newPostsByAccount.size());
			log.debug("account: {} - new posts: {}", account.accountNickname(), newPostsByAccount);

			AccountPost lastPost = newPostsByAccount.stream()
					.max(Comparator.comparing(AccountPost::id))
					.orElseThrow();
			repo.updateLastPostPkByAccount(account, lastPost);
			log.info("account: {}, saved last post: {}", account.accountNickname(), lastPost);
		}
	}
}
