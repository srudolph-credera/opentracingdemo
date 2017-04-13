package com.credera.demo.opentracing.cross_section;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.ActivityLevels;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.ActivityLevels.Builder;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.Range;

import java.io.IOException;
import java.util.logging.Logger;

class CrossSectionServer {
    private static final Logger logger = Logger.getLogger(CrossSectionServer.class.getName());

    private final int port;
    private final Server server;

    CrossSectionServer(int port) throws IOException {
        this(ServerBuilder.forPort(port), port);
    }

    private CrossSectionServer(ServerBuilder<?> serverBuilder, int port) {
        this.port = port;
        this.server = serverBuilder.addService(new CrossSectionService()).build();
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
        private ActivityLevels requestActivityLevels(Range request) {
            Builder builder = ActivityLevels.newBuilder();
            builder.addLevel(0.5);
            builder.addLevel(0.3);
            builder.addLevel(0.7);
            return builder.build();
        }

        @Override
        public void getCrossSection(Range request, StreamObserver<ActivityLevels> responseObserver) {
            responseObserver.onNext(requestActivityLevels(request));
            responseObserver.onCompleted();
        }
    }
}