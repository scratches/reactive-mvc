package com.example;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@SpringBootApplication
@RestController
public class ReactiveApplication {

	@GetMapping("/words")
	public Flux<String> words() {
		return Flux.fromArray(new String[] { "foo", "bar" });
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

	@GetMapping("/sentences")
	public Flux<List<String>> sentences() {
		return Flux.just(Arrays.asList("go", "home"), Arrays.asList("come", "back"));
	}

	public static void main(String[] args) {
		SpringApplication.run(ReactiveApplication.class, args);
	}
}
