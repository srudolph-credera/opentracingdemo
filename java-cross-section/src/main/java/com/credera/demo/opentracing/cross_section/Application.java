package com.credera.demo.opentracing.cross_section;

import io.opentracing.Tracer;
import io.opentracing.contrib.okhttp3.SpanDecorator;
import io.opentracing.contrib.okhttp3.TracingInterceptor;
import brave.opentracing.BraveTracer;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.okhttp3.OkHttpSender;

import java.util.Arrays;

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
                .localServiceName("Java Cross Section")
                .reporter(reporter)
                .build();

        // Finally, wrap this with the OpenTracing API
        Tracer tracer = BraveTracer.wrap(braveTracer);

        // Create HTTP client for accessing activity data from heatmap service
        TracingInterceptor tracingInterceptor = new TracingInterceptor(tracer, Arrays.asList(SpanDecorator.STANDARD_TAGS));
        OkHttpClient client =  (new Builder())
                .addInterceptor(tracingInterceptor)
                .addNetworkInterceptor(tracingInterceptor)
                .build();

        CrossSectionServer server = new CrossSectionServer(8082, tracer, client);
        server.start();
        server.blockUntilShutdown();
    }
}
