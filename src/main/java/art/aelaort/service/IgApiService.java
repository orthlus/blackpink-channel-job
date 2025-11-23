package art.aelaort.service;

import art.aelaort.dto.db.Account;
import art.aelaort.dto.ig.AccountPost;
import art.aelaort.exceptions.LastPostOrStoryPkNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IgApiService {
	private final Repo repo;
	private final IgRequestService igRequestService;

	public List<AccountPost> getNewPostsByAccount(Account account) {
		return getNewPostsByAccount(account, Integer.MAX_VALUE);
	}

	public List<AccountPost> getNewPostsByAccount(Account account, int apiPageLimit) {
		List<AccountPost> result = new ArrayList<>();
		String lastPostId;
		try {
			lastPostId = repo.getLastPostPk(account);
		} catch (LastPostOrStoryPkNotFoundException e) {
			AccountPost accountPost = igRequestService.getPostsByAccount(account).getFirst();
			return List.of(accountPost);
		}

		List<AccountPost> postsByAccount = igRequestService.getPostsByAccount(account);
		if (postsHasPostId(postsByAccount, lastPostId)) {
			List<AccountPost> newPosts = filterMoreThenId(postsByAccount, lastPostId);

			result.addAll(newPosts);
		} else {
			result.addAll(postsByAccount);

			List<AccountPost> prevPosts = igRequestService.getPostsByAccount(account, getMinimalId(postsByAccount));

			int pageCount = 1;
			while (true) {
				if (postsHasPostId(prevPosts, lastPostId)) {
					List<AccountPost> newPosts = filterMoreThenId(prevPosts, lastPostId);

					result.addAll(newPosts);
					break;
				} else {
					if (pageCount < apiPageLimit) {
						result.addAll(prevPosts);
						prevPosts = igRequestService.getPostsByAccount(account, getMinimalId(prevPosts));
						pageCount++;
					} else {
						break;
					}
				}
			}
		}

		return result;
	}

	private String getMinimalId(List<AccountPost> posts) {
		AccountPost minimalPost = posts.getFirst();
		for (AccountPost post : posts) {
			if (bi(post.pk()).compareTo(bi(minimalPost.pk())) < 0) {
				minimalPost = post;
			}
		}
		return minimalPost.id();
	}

	private List<AccountPost> filterMoreThenId(List<AccountPost> posts, String id) {
		BigInteger idToFilter = bi(id);
		return posts.stream()
				.filter(post -> bi(post.pk()).compareTo(idToFilter) > 0)
				.toList();
	}

	private BigInteger bi(String val) {
		return new BigInteger(val);
	}

	private boolean postsHasPostId(List<AccountPost> posts, String postId) {
		for (AccountPost post : posts) {
			if (post.pk().equals(postId)) {
				return true;
			}
		}
		return false;
	}
}
