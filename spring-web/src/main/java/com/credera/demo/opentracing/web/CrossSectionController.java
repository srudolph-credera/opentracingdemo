package com.credera.demo.opentracing.web;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@RestController
public class CrossSectionController {
    private static final Logger LOG = LoggerFactory.getLogger(CrossSectionController.class);

    @Autowired
    private Tracer tracer;

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
        String requestUrl = String.format(
                "http://localhost:8082/section?minX=%d&minY=%d&maxX=%d&maxY=%d", minX, minY, maxX, maxY);
        RestTemplate heatMapTemplate = new RestTemplate();
        heatMapTemplate.setInterceptors(Collections.singletonList(new TracingRestTemplateInterceptor(tracer)));
        ResponseEntity<Double[]> response = heatMapTemplate.getForEntity(requestUrl, Double[].class);
        return response.getBody();
    }
}

