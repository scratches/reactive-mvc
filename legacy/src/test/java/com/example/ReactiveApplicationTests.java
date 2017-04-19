package com.example;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ReactiveApplicationTests {

	private static final MediaType EVENT_STREAM = MediaType.TEXT_EVENT_STREAM;
	@LocalServerPort
	private int port;
	@Autowired
	private TestRestTemplate rest;

	@Test
	public void wordsSSE() throws Exception {
		assertThat(rest.exchange(
				RequestEntity.get(new URI("/words")).accept(EVENT_STREAM).build(),
				String.class).getBody()).isEqualTo(sse("foo", "bar"));
	}

	@Test
	public void wordsJson() throws Exception {
		assertThat(rest
				.exchange(RequestEntity.get(new URI("/words"))
						.accept(MediaType.APPLICATION_JSON).build(), String.class)
				.getBody()).isEqualTo("[\"foo\",\"bar\"]");
	}

	@Test
	public void errorJson() throws Exception {
		ResponseEntity<String> result = rest.exchange(RequestEntity.get(new URI("/bang"))
				.accept(MediaType.APPLICATION_JSON).build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(result.getBody()).contains("status");
	}

	@Test
	public void words() throws Exception {
		assertThat(
				rest.exchange(RequestEntity.get(new URI("/words")).build(), String.class)
						.getBody()).isEqualTo("foobar");
	}

	@Test
	public void emptyJson() throws Exception {
		assertThat(rest
				.exchange(RequestEntity.get(new URI("/empty"))
						.accept(MediaType.APPLICATION_JSON).build(), String.class)
				.getBody()).isEqualTo("[]");
	}

	@Test
	public void sentences() throws Exception {
		// RestTemplate will send Accept: text/plain, ... application/json, ... */* so we
		// end up trying to render the String as text/plain and failing
		assertThat(rest
				.exchange(RequestEntity.get(new URI("/sentences")).build(), String.class)
				.getBody()).isEqualTo("[[\"go\",\"home\"],[\"come\",\"back\"]]");
	}

	@Test
	public void sentencesAcceptAny() throws Exception {
		assertThat(rest.exchange(
				RequestEntity.get(new URI("/sentences")).accept(MediaType.ALL).build(),
				String.class).getBody())
						.isEqualTo("[[\"go\",\"home\"],[\"come\",\"back\"]]");
	}

	@Test
	public void sentencesAcceptJson() throws Exception {
		ResponseEntity<String> result = rest.exchange(
				RequestEntity.get(new URI("http://localhost:" + port + "/sentences"))
						.accept(MediaType.APPLICATION_JSON).build(),
				String.class);
		assertThat(result.getBody()).isEqualTo("[[\"go\",\"home\"],[\"come\",\"back\"]]");
		assertThat(result.getHeaders().getContentType()
				.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
	}

	private String sse(String... values) {
		return "data:" + StringUtils.arrayToDelimitedString(values, "\n\ndata:") + "\n\n";
	}

}
