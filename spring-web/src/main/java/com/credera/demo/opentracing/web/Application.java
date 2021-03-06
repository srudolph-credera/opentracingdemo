package com.credera.demo.opentracing.web;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.opentracing.Tracer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;

@SpringBootApplication
public class Application {
    @Bean
    Tracer zipkinTracer() {
        // Configure a reporter, which controls how often spans are sent
        //   (the dependency is io.zipkin.reporter:zipkin-sender-okhttp3)
        OkHttpSender sender = OkHttpSender.create("http://127.0.0.1:9411/api/v1/spans");
        AsyncReporter reporter = AsyncReporter.builder(sender).build();

        // Now, create a Brave tracing component with the service name you want to see in Zipkin.
        //   (the dependency is io.zipkin.brave:brave)
        Tracing braveTracing = Tracing.newBuilder()
                .localServiceName("Spring Boot Web")
                .reporter(reporter)
                .build();

        // Finally, wrap this with the OpenTracing API
        return BraveTracer.create(braveTracing);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
