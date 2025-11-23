package art.aelaort.service;

import art.aelaort.BlackpinkChannelJob;
import art.aelaort.dto.db.Account;
import art.aelaort.exceptions.AccountToScanNotFoundException;
import art.aelaort.tables.AccountsToScan;
import art.aelaort.tables.LastPosts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.jooq.Records.mapping;


@Slf4j
@Component
@RequiredArgsConstructor
public class Repo {
	private final DSLContext db;
	private final AccountsToScan ats = BlackpinkChannelJob.BLACKPINK_CHANNEL_JOB.ACCOUNTS_TO_SCAN;
	private final LastPosts lp = BlackpinkChannelJob.BLACKPINK_CHANNEL_JOB.LAST_POSTS;

	public Set<Account> getAccountsByInterval(String interval) {
		return db.select(
						ats.ID,
						ats.ACCOUNT_ID,
						ats.ACCOUNT_NICKNAME,
						ats.ACCOUNT_TITLE,
						ats.SCAN_INTERVAL,
						ats.FETCH_STORIES
				)
				.from(ats)
				.where(ats.SCAN_INTERVAL.eq(interval))
				.fetchSet(mapping(Account::new));
	}

	public Set<String> getAccountsIdsByInterval(String interval) {
		return db.select(ats.ACCOUNT_ID)
				.from(ats)
				.where(ats.SCAN_INTERVAL.eq(interval))
				.fetchSet(ats.ACCOUNT_ID);
	}

	public Account getAccountById(String accountId) {
		return db.select(
						ats.ID,
						ats.ACCOUNT_ID,
						ats.ACCOUNT_NICKNAME,
						ats.ACCOUNT_TITLE,
						ats.SCAN_INTERVAL,
						ats.FETCH_STORIES
				)
				.from(ats)
				.where(ats.ACCOUNT_ID.eq(accountId))
				.fetchOptional(mapping(Account::new))
				.orElseThrow(AccountToScanNotFoundException::new);
	}

	public void updateLastPostIdByAccount(Account account, String lastPostId, boolean isStory) {
		db.insertInto(lp)
				.columns(lp.SCAN_ACCOUNT_ID, lp.LAST_SEND_POST_ID, lp.IS_STORY)
				.values(account.id(), lastPostId, isStory)
				.onConflict(lp.SCAN_ACCOUNT_ID)
				.doUpdate()
				.set(lp.LAST_SEND_POST_ID, lastPostId)
				.set(lp.IS_STORY, isStory)
				.where(lp.SCAN_ACCOUNT_ID.eq(account.id()))
				.execute();
	}
}
