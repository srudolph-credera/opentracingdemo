package com.credera.demo.opentracing.web;

import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrossSectionController {
    private static final Logger LOG = LoggerFactory.getLogger(CrossSectionController.class);

    @Autowired
    private Tracer tracer;

    @Autowired
    private CrossSectionClient crossSectionClient;

    @RequestMapping("/section")
    public Double[] crossSection(
            @RequestParam(value = "minX") Integer minX,
            @RequestParam(value = "minY") Integer minY,
            @RequestParam(value = "maxX") Integer maxX,
            @RequestParam(value = "maxY") Integer maxY) {
        if (minX == null) {
            LOG.error("minX is null");
            return null;
        }

        if (minY == null) {
            LOG.error("minY is null");
            return null;
        }

        if (maxX == null) {
            LOG.error("maxX is null");
            return null;
        }

        if (maxY == null) {
            LOG.error("maxY is null");
            return null;
        }

        // Forward request to Clojure Cross Section service
        return crossSectionClient.getCrossSection(minX, minY, maxX, maxY);
    }
}

