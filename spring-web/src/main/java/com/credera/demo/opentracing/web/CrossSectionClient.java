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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
class CrossSectionClient {
    private static final Logger LOG = LoggerFactory.getLogger(CrossSectionController.class);

    @Autowired
    private Tracer tracer;

    private CrossSectionBlockingStub blockingStub;

    @Value("${grpc.host:localhost}")
    private String grpcHost;

    @Value("${grpc.port:8082}")
    private int grpcPort;

    @PostConstruct
    public void init() {
        ManagedChannelBuilder channelBuilder = ManagedChannelBuilder.forAddress(grpcHost, grpcPort).usePlaintext(true);
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
