package com.credera.demo.opentracing.web;

import com.credera.demo.opentracing.cross_section.CrossSectionGrpc;
import com.credera.demo.opentracing.cross_section.CrossSectionGrpc.CrossSectionBlockingStub;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.ActivityLevels;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.Point;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.Range;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.opentracing.Tracer;
import io.opentracing.contrib.ClientTracingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CrossSectionClient {
    private static final Logger LOG = LoggerFactory.getLogger(CrossSectionController.class);

    private final CrossSectionBlockingStub blockingStub;

    CrossSectionClient(String host, int port, Tracer tracer) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true), tracer);
    }

    private CrossSectionClient(ManagedChannelBuilder<?> channelBuilder, Tracer tracer) {
        ClientTracingInterceptor interceptor = new ClientTracingInterceptor(tracer);
        this.blockingStub = CrossSectionGrpc.newBlockingStub(
                interceptor.intercept(channelBuilder.build()));
    }

    Double[] getCrossSection(int minX, int minY, int maxX, int maxY) {
        Range range = Range.newBuilder()
                .setStart(Point.newBuilder().setX(minX).setY(minY).build())
                .setEnd(Point.newBuilder().setX(maxX).setY(maxY).build()).build();
        ActivityLevels levels;
        try {
            levels = blockingStub.getCrossSection(range);
        } catch (StatusRuntimeException e) {
            LOG.warn("RPC failed: %s", e.getStatus());
            return null;
        }

        return levels.getLevelList().stream().toArray(Double[]::new);
    }
}
