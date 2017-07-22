package com.credera.demo.opentracing.cross_section;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opentracing.Span;
import io.opentracing.Tracer;

import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.ActivityLevels;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.ActivityLevels.Builder;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.Range;
import io.opentracing.contrib.OpenTracingContextKey;
import io.opentracing.contrib.ServerTracingInterceptor;
import io.opentracing.contrib.okhttp3.TagWrapper;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

class CrossSectionServer {
    private static final Logger logger = Logger.getLogger(CrossSectionServer.class.getName());

    private final int port;
    private final Server server;

    CrossSectionServer(int port, Tracer tracer, Call.Factory client) throws IOException {
        this(ServerBuilder.forPort(port), port, tracer, client);
    }

    private CrossSectionServer(ServerBuilder<?> serverBuilder, int port, Tracer tracer, Call.Factory client) {
        this.port = port;
        ServerTracingInterceptor interceptor = new ServerTracingInterceptor(tracer);
        this.server = serverBuilder
                .addService(interceptor.intercept(new CrossSectionService(client)))
                .build();
    }

    void start() throws IOException {
        server.start();
        logger.info("Server started, listening on :" + port);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private static class CrossSectionService extends CrossSectionGrpc.CrossSectionImplBase {
        private final Call.Factory httpClient;

        CrossSectionService(Call.Factory client) {
            this.httpClient = client;
        }

        private static Double safeDoubleFromBody(ResponseBody body) {
            Double d = 0.0;
            try {
                d = Double.parseDouble(body.string());
            } catch(IOException e) {
                logger.warning("Error retrieving activity levels: " + e);
            }

            return d;
        }

        private ActivityLevels requestActivityLevels(Range request, Span span) {
            int startX = request.getStart().getX();
            double deltaX = request.getEnd().getX() - startX;
            int startY = request.getStart().getY();
            double deltaY = request.getEnd().getY() - startY;
            double length = Math.sqrt(Math.pow(deltaX, 2.0) + Math.pow(deltaY, 2.0));
            double stepX = deltaX / length;
            double stepY = deltaY / length;
            ArrayList<CompletableFuture<Response>> responses = new ArrayList<>((int)Math.floor(length) + 1);
            for (int i = 0; i < (int)Math.floor(length) + 1; i++) {
                int x = startX + (int)(i*stepX);
                int y = startY + (int)(i*stepY);
                OkHttpResponseFuture callback = new OkHttpResponseFuture();
                Request activityRequest = new Request.Builder()
                        .url("http://localhost:8081/heatmap?x=" + x + "&y=" + y)
                        .tag(new TagWrapper(span.context()))
                        .build();
                this.httpClient.newCall(activityRequest).enqueue(callback);
                responses.add(callback.future);
            }

            Builder builder = ActivityLevels.newBuilder();
            responses.stream()
                    .parallel()
                    .map(CompletableFuture::join)
                    .map(Response::body)
                    .map(CrossSectionService::safeDoubleFromBody)
                    .forEachOrdered(builder::addLevel);
            return builder.build();
        }

        @Override
        public void getCrossSection(Range request, StreamObserver<ActivityLevels> responseObserver) {
            Span span = OpenTracingContextKey.activeSpan();
            responseObserver.onNext(requestActivityLevels(request, span));
            responseObserver.onCompleted();
        }
    }
}