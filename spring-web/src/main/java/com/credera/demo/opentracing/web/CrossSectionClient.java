package com.credera.demo.opentracing.web;

import com.credera.demo.opentracing.cross_section.CrossSectionGrpc;
import com.credera.demo.opentracing.cross_section.CrossSectionGrpc.CrossSectionBlockingStub;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.ActivityLevels;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.Point;
import com.credera.demo.opentracing.cross_section.CrossSectionOuterClass.Range;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CrossSectionClient {
    private static final Logger LOG = LoggerFactory.getLogger(CrossSectionController.class);

    private final ManagedChannel channel;
    private final CrossSectionBlockingStub blockingStub;
    public CrossSectionClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    private CrossSectionClient(ManagedChannelBuilder<?> channelBuilder) {
        this.channel = channelBuilder.build();
        this.blockingStub = CrossSectionGrpc.newBlockingStub(this.channel);
    }

    public Double[] getCrossSection(int minX, int minY, int maxX, int maxY) {
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
