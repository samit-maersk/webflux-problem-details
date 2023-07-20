package com.example.webfluxproblemdetails;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

import static org.springframework.util.StringUtils.hasText;

@SpringBootApplication
public class WebfluxProblemDetailsApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxProblemDetailsApplication.class, args);
	}

	@Bean
	RouterFunction routerFunction() {
		return RouterFunctions
				.route()
				.GET("/greet", request -> {
					var param = request.queryParam("message").orElse("");
					if(hasText(param)) {
						var res = Mono.just("Hello %s".formatted(param));
						return ServerResponse.ok().body(res, String.class);
					} else {
						return Mono.error(new CustomException("Custom Exception"));
					}
				})
				.filter((request, next) -> next
						.handle(request)
						.onErrorResume(CustomException.class, e -> {
							ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,e.getLocalizedMessage());
							problemDetails.setType(URI.create("http://localhost:8080/errors/customer-not-found"));
							problemDetails.setTitle(e.getMessage());
							// Adding non-standard property
							problemDetails.setProperty("customerId", UUID.randomUUID().toString());
							return ServerResponse.status(500).bodyValue(problemDetails);
				}))
				.build();
	}


//	@Bean
//	WebFilter dataNotFoundToBadRequest() {
//		return (exchange, next) -> next.filter(exchange)
//				.onErrorResume(DataNotFoundException.class, e -> {
//					ServerHttpResponse response = exchange.getResponse();
//					response.setStatusCode(HttpStatus.BAD_REQUEST);
//					return response.setComplete();
//				});
//	}
}

@RestController
class Routers {
	@GetMapping("/greet/v2")
	public Mono<ResponseEntity<String>> greet(@RequestParam(value = "message", defaultValue = "") String message) {
		return Mono.fromCallable(() -> {
			if (hasText(message)) {
				return new ResponseEntity<>("Hello %s".formatted(message), HttpStatus.OK);
			}
			throw new CustomException("custom exception");
		});
	}
}

class CustomException extends RuntimeException {
	public CustomException(String message) {
		super(message);
	}
	public CustomException(String message,Throwable t) {
		super(message,t);
	}
}

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ProblemDetail handleRecordNotFoundException(CustomException ex) {
		ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,ex.getLocalizedMessage());
		problemDetails.setType(URI.create("http://localhost:8080/errors/customer-not-found"));
		problemDetails.setTitle(ex.getMessage());
		// Adding non-standard property
		problemDetails.setProperty("customerId", UUID.randomUUID().toString());
		return problemDetails;
	}
}
