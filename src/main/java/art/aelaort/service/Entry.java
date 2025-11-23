package art.aelaort.service;

import art.aelaort.dto.db.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class Entry implements CommandLineRunner {
	private final Repo repo;
	@Value("${scan.interval}")
	private String scanInterval;

	@Override
	public void run(String... args) throws Exception {
		Set<Account> accounts = repo.getAccountsByInterval(scanInterval);
	}
}
