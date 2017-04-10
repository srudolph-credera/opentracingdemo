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
public class HeatmapController {
    private static final Logger LOG = LoggerFactory.getLogger(HeatmapController.class);

    @Autowired
    private Tracer tracer;

    @RequestMapping("/heatmap")
    public double heatmap(@RequestParam(value = "x") Integer x, @RequestParam(value = "y") Integer y) {
        if (x == null) {
            LOG.error("x is null");
            return 0;
        }

        if (y == null) {
            LOG.error("y is null");
            return 0;
        }

        // Forward request to Go Heat Map service
        String requestUrl = String.format("http://localhost:8081/heatmap?x=%d&y=%d", x, y);
        RestTemplate heatMapTemplate = new RestTemplate();
        heatMapTemplate.setInterceptors(Collections.singletonList(new TracingRestTemplateInterceptor(tracer)));
        ResponseEntity<String> response = heatMapTemplate.getForEntity(requestUrl, String.class);

        return Double.parseDouble(response.getBody());
    }
}
