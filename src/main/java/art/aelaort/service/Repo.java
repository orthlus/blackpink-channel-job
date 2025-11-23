package art.aelaort.service;

import art.aelaort.BlackpinkChannelJob;
import art.aelaort.dto.db.Account;
import art.aelaort.dto.ig.AccountPost;
import art.aelaort.exceptions.LastPostOrStoryPkNotFoundException;
import art.aelaort.tables.AccountsToScan;
import art.aelaort.tables.LastPosts;
import art.aelaort.tables.LastStories;
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
	private final LastStories ls = BlackpinkChannelJob.BLACKPINK_CHANNEL_JOB.LAST_STORIES;

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
				.where(ats.SCAN_INTERVAL.eq(interval).and(
						ats.IS_SCAN_DISABLED.eq(false)
				))
				.fetchSet(mapping(Account::new));
	}

	public String getLastPostPk(Account account) {
		return db.select(lp.LAST_SEND_POST_PK)
				.from(lp)
				.where(lp.SCAN_ACCOUNT_ID.eq(account.id()))
				.fetchOptional(lp.LAST_SEND_POST_PK)
				.orElseThrow(LastPostOrStoryPkNotFoundException::new);
	}

	public void updateLastPostPkByAccount(Account account, AccountPost lastPost) {
		db.insertInto(lp)
				.columns(lp.SCAN_ACCOUNT_ID, lp.LAST_SEND_POST_PK)
				.values(account.id(), lastPost.pk())
				.onConflict(lp.SCAN_ACCOUNT_ID)
				.doUpdate()
				.set(lp.LAST_SEND_POST_PK, lastPost.pk())
				.where(lp.SCAN_ACCOUNT_ID.eq(account.id()))
				.execute();
	}

	public String getLastStoryId(Account account) {
		return db.select(ls.LAST_SEND_STORY_PK)
				.from(ls)
				.where(ls.SCAN_ACCOUNT_ID.eq(account.id()))
				.fetchOptional(ls.LAST_SEND_STORY_PK)
				.orElseThrow(LastPostOrStoryPkNotFoundException::new);
	}

	public void updateLastStoryIdByAccount(Account account, String lastStoryId) {
		db.insertInto(ls)
				.columns(ls.SCAN_ACCOUNT_ID, ls.LAST_SEND_STORY_PK)
				.values(account.id(), lastStoryId)
				.onConflict(ls.SCAN_ACCOUNT_ID)
				.doUpdate()
				.set(ls.LAST_SEND_STORY_PK, lastStoryId)
				.where(ls.SCAN_ACCOUNT_ID.eq(account.id()))
				.execute();
	}
}
