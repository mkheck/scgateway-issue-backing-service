package com.example.backingservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.UUID;

@EnableDiscoveryClient
@SpringBootApplication
public class BackingServiceApplication {
    @Bean
    CommandLineRunner demoData(QuoteRepository repo) {
        return args -> {
            Flux<String> quoteTexts = Flux.just(
                    "These go to eleven.",
                    "All you need to start an asylum is an empty room and the right kind of people.",
                    "The Dude abides.",
                    "Hasta la vista, baby.",
                    "What we've got here is a failure to communicate.",
                    "Roads? Where we're going we don't need roads.",
                    "Houston, we have a problem.",
                    "To infinity and beyond!",
                    "Hello. My name is Inigo Montoya. You killed my father. Prepare to die.");
            Flux<String> quoteSources = Flux.just(
                    "This Is Spinal Tap, 1984",
                    "My Man Godfrey, 1936",
                    "The Big Lebowski, 1998",
                    "Terminator 2: Judgment Day, 1991",
                    "Cool Hand Luke, 1967",
                    "Back to the Future, 1985",
                    "Apollo 13, 1995",
                    "Toy Story, 1995",
                    "The Princess Bride, 1987");

            repo.deleteAll().thenMany(
                    Flux.zip(quoteSources, quoteTexts)
                            .map(t -> new Quote(UUID.randomUUID().toString(), t.getT1(), t.getT2()))
                            .flatMap(repo::save))
                    .thenMany(repo.findAll())
                    .subscribe(System.out::println);
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(BackingServiceApplication.class, args);
    }
}

@RestController
@RequestMapping("/quotes")
class QuoteController {
    private final QuoteRepository repo;

    QuoteController(QuoteRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Flux<Quote> getAllQuotes() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Mono<Quote> getQuoteById(@PathVariable String id) {
        return repo.findById(id);
    }

    @GetMapping("/random")
    public Mono<Quote> getRandomQuote() {
        return repo.count()
                .map(Long::intValue)
                .map(new Random()::nextInt)
                .flatMap(repo.findAll()::elementAt);
    }
}

interface QuoteRepository extends ReactiveCrudRepository<Quote, String> {
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class Quote {
    @Id
    private String id;
    private String source, text;
}