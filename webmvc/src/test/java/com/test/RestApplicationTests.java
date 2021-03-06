/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.test;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.test.RestApplicationTests.TestConfiguration;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Dave Syer
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class RestApplicationTests {

	private static final MediaType EVENT_STREAM = MediaType.valueOf("text/event-stream");
	@LocalServerPort
	private int port;
	@Autowired
	private TestRestTemplate rest;
	@Autowired
	private TestConfiguration test;

	@Before
	public void init() {
		test.list.clear();
	}

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
	@Ignore("Fix error handling")
	public void errorJson() throws Exception {
		assertThat(rest
				.exchange(RequestEntity.get(new URI("/bang"))
						.accept(MediaType.APPLICATION_JSON).build(), String.class)
				.getBody()).isEqualTo("[\"foo\"]");
	}

	@Test
	public void words() throws Exception {
		ResponseEntity<String> result = rest
				.exchange(RequestEntity.get(new URI("/words")).build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("foobar");
	}

	@Test
	public void getMore() throws Exception {
		ResponseEntity<String> result = rest
				.exchange(RequestEntity.get(new URI("/get/more")).build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).isEqualTo("foobar");
	}

	@Test
	public void updates() throws Exception {
		ResponseEntity<String> result = rest.exchange(RequestEntity
				.post(new URI("/updates")).contentType(MediaType.APPLICATION_JSON)
				.body("[\"one\",\"two\"]"), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(test.list).hasSize(2);
		assertThat(result.getBody()).isEqualTo("onetwo");
	}

	@Test
	public void timeout() throws Exception {
		assertThat(rest
				.exchange(RequestEntity.get(new URI("/timeout")).build(), String.class)
				.getBody()).isEqualTo("foo");
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
		ResponseEntity<String> result = rest
				.exchange(
						RequestEntity.get(new URI("/sentences"))
								.accept(MediaType.APPLICATION_JSON).build(),
						String.class);
		assertThat(result.getBody()).isEqualTo("[[\"go\",\"home\"],[\"come\",\"back\"]]");
		assertThat(result.getHeaders().getContentType())
				.isGreaterThanOrEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	public void sentencesAcceptSse() throws Exception {
		ResponseEntity<String> result = rest.exchange(
				RequestEntity.get(new URI("/sentences")).accept(EVENT_STREAM).build(),
				String.class);
		assertThat(result.getBody())
				.isEqualTo(sse("[\"go\",\"home\"]", "[\"come\",\"back\"]"));
		assertThat(result.getHeaders().getContentType().isCompatibleWith(EVENT_STREAM))
				.isTrue();
	}

	@Test
	public void uppercase() throws Exception {
		ResponseEntity<String> result = rest.exchange(RequestEntity
				.post(new URI("/uppercase")).contentType(MediaType.APPLICATION_JSON)
				.body("[\"foo\",\"bar\"]"), String.class);
		assertThat(result.getBody()).isEqualTo("[\"(FOO)\",\"(BAR)\"]");
	}

	@Test
	public void uppercaseGet() {
		assertThat(rest.getForObject("/uppercase/foo", String.class)).isEqualTo("[FOO]");
	}

	@Test
	public void convertGet() {
		assertThat(rest.getForObject("/wrap/123", String.class)).isEqualTo("..123..");
	}

	@Test
	public void uppercaseJsonStream() throws Exception {
		assertThat(rest
				.exchange(RequestEntity.post(new URI("/maps"))
						.contentType(MediaType.APPLICATION_JSON)
						.body("[{\"value\":\"foo\"},{\"value\":\"bar\"}]"), String.class)
				.getBody()).isEqualTo("[{\"value\":\"FOO\"},{\"value\":\"BAR\"}]");
	}

	@Test
	public void uppercaseSSE() throws Exception {
		assertThat(
				rest.exchange(
						RequestEntity.post(new URI("/uppercase")).accept(EVENT_STREAM)
								.contentType(EVENT_STREAM).body(sse("foo", "bar")),
						String.class).getBody()).isEqualTo(sse("(FOO)", "(BAR)"));
	}

	private String sse(String... values) {
		return "data:" + StringUtils.arrayToDelimitedString(values, "\n\ndata:") + "\n\n";
	}

	@EnableAutoConfiguration
	@RestController
	@Configuration
	public static class TestConfiguration {

		private List<String> list = new ArrayList<>();

		@PostMapping({ "/uppercase", "/transform", "/post/more" })
		public Flux<String> uppercase(@RequestBody List<String> list) {
			Flux<String> flux = Flux.fromIterable(list);
			return flux.log().map(value -> "(" + value.trim().toUpperCase() + ")");
		}

		@GetMapping("/uppercase/{id}")
		public Mono<String> uppercaseGet(@PathVariable String id) {
			return Mono.just(id).map(value -> "[" + value.trim().toUpperCase() + "]");
		}

		@PostMapping("/wrap")
		public Flux<String> wrap(@RequestBody List<String> list) {
			Flux<String> flux = Flux.fromIterable(list);
			return flux.log().map(value -> ".." + value + "..");
		}

		@GetMapping("/wrap/{id}")
		public Mono<String> wrapGet(@PathVariable int id) {
			return Mono.just(id).log().map(value -> ".." + value + "..");
		}

		@PostMapping("/entity")
		public Flux<Map<String, Object>> entity(@RequestBody List<Integer> list) {
			Flux<Integer> flux = Flux.fromIterable(list);
			return flux.log().map(value -> Collections.singletonMap("value", value));
		}

		@PostMapping("/maps")
		public Flux<Map<String, String>> maps(
				@RequestBody List<HashMap<String, String>> list) {
			Flux<HashMap<String, String>> flux = Flux.fromIterable(list);
			return flux.map(value -> {
				value.put("value", value.get("value").trim().toUpperCase());
				return value;
			});
		}

		@GetMapping({ "/words", "/get/more" })
		public Flux<String> words() {
			return Flux.fromArray(new String[] { "foo", "bar" });
		}

		@PostMapping("/updates")
		public ResponseEntity<Flux<String>> updates(@RequestBody List<String> list) {
			Flux<String> flux = Flux.fromIterable(list);
			flux = flux.cache();
			flux.subscribe(value -> this.list.add(value));
			return ResponseEntity.accepted().body(flux);
		}

		@GetMapping("/bang")
		public Flux<String> bang() {
			return Flux.fromArray(new String[] { "foo", "bar" }).map(value -> {
				if (value.equals("bar")) {
					throw new RuntimeException("Bar");
				}
				return value;
			});
		}

		@GetMapping("/empty")
		public Flux<String> empty() {
			return Flux.fromIterable(Collections.emptyList());
		}

		@GetMapping("/timeout")
		public Flux<String> timeout() {
			return Flux.defer(() -> Flux.<String>create(emitter -> {
				emitter.next("foo");
			}).timeout(Duration.ofMillis(100L), Flux.empty()));
		}

		@GetMapping("/sentences")
		public Flux<List<String>> sentences() {
			return Flux.just(Arrays.asList("go", "home"), Arrays.asList("come", "back"));
		}

	}

}
