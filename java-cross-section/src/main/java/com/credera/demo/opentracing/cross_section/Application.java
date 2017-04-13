package com.credera.demo.opentracing.cross_section;

import io.opentracing.Tracer;
import brave.opentracing.BraveTracer;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;

public class Application {

    public static void main(String[] args) throws Exception {
        // Initialize Zipkin

        // Configure a reporter, which controls how often spans are sent
        //   (the dependency is io.zipkin.reporter:zipkin-sender-okhttp3)
        OkHttpSender sender = OkHttpSender.create("http://127.0.0.1:9411/api/v1/spans");
        AsyncReporter reporter = AsyncReporter.builder(sender).build();

        // Now, create a Brave tracer with the service name you want to see in Zipkin.
        //   (the dependency is io.zipkin.brave:brave)
        brave.Tracer braveTracer = brave.Tracer.newBuilder()
                .localServiceName("spring-boot-web")
                .reporter(reporter)
                .build();

        // Finally, wrap this with the OpenTracing API
        Tracer tracer = BraveTracer.wrap(braveTracer);

        CrossSectionServer server = new CrossSectionServer(8082);
        server.start();
        server.blockUntilShutdown();
    }
}
