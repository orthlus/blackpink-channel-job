package main;

import art.aelaort.dto.ig.AccountPost;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IgJsonTest {
	@ParameterizedTest
	@ValueSource(strings = {"input-json.json"})
	void testIgJson1(String file) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		AccountPost[] posts = objectMapper.readValue(getClass().getClassLoader().getResource(file), AccountPost[].class);
		assertEquals(2, posts.length);
		assertEquals("3754672899627228091_7140177628", posts[0].id());
	}

	@ParameterizedTest
	@ValueSource(strings = {"input-json-wrap-arr.json"})
	void testIgJson2(String file) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<Object> result = objectMapper.readValue(getClass().getClassLoader().getResource(file), listObjectTypeReference());
		AccountPost[] posts = objectMapper.convertValue(result.get(0), AccountPost[].class);
		assertEquals(2, posts.length);
		assertEquals("3754672899627228091_7140177628", posts[0].id());
	}

	private static TypeReference<List<Object>> listObjectTypeReference() {
		return new TypeReference<>() {
		};
	}
}
