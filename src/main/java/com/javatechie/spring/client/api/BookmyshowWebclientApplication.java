package com.javatechie.spring.client.api;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
@RequestMapping("/bookMyShow-client")
public class BookmyshowWebclientApplication {

	Logger log = LoggerFactory.getLogger(BookmyshowWebclientApplication.class);

	WebClient webClient;

	@PostConstruct
	public void init() {
		webClient = WebClient.builder().baseUrl("http://localhost:9090/BookMyShow/Service")
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).filter(logRequest())
				.filter(logResponse()).build();
	}

	@PostMapping("/bookNow")
	public Mono<String> BookNow(@RequestBody BookRequest request) {
		return webClient.post().uri("/bookingShow").syncBody(request).retrieve().bodyToMono(String.class);
	}

	@GetMapping("/trackBookings")
	public Flux<BookRequest> trackAllBooking() {
		return webClient.get().uri("/getAllBooking").retrieve().bodyToFlux(BookRequest.class);
	}

	@GetMapping("/trackBooking/{bookingId}")
	public Mono<BookRequest> getBookingById(@PathVariable int bookingId) {
		return webClient.get().uri("/getBooking/" + bookingId).retrieve()
				.onStatus(HttpStatus::is4xxClientError,
						clinetResponse -> Mono.error(new BookMyShowClientException(" 404 unsported exception")))
				.onStatus(HttpStatus::is5xxServerError,
						clinetResponse -> Mono.error(new BookMyShowClientException(" 505 Server exception")))
				.bodyToMono(BookRequest.class);
	}

	@DeleteMapping("/removeBooking/{bookingId}")
	public Mono<String> cancelBooking(@PathVariable int bookingId) {
		return webClient.delete().uri("/cancelBooking/" + bookingId).retrieve().bodyToMono(String.class);
	}

	@PutMapping("/changeBooking/{bookingId}")
	public Mono<BookRequest> updateBooking(@PathVariable int bookingId, @RequestBody BookRequest request) {
		return webClient.put().uri("/updateBooking/" + bookingId).syncBody(request).retrieve()
				.bodyToMono(BookRequest.class);
	}

	private ExchangeFilterFunction logRequest() {
		return ExchangeFilterFunction.ofRequestProcessor(clinetRequest -> {
			log.info("Request {} {}", clinetRequest.method(), clinetRequest.url());
			return Mono.just(clinetRequest);
		});
	}

	private ExchangeFilterFunction logResponse() {
		return ExchangeFilterFunction.ofResponseProcessor(clinetResponse -> {
			log.info("Response status code {} ", clinetResponse.statusCode());
			return Mono.just(clinetResponse);
		});
	}

	public static void main(String[] args) {
		SpringApplication.run(BookmyshowWebclientApplication.class, args);
	}
}
