package com.credera.demo.opentracing.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class HeatmapController {
    private static final Logger LOG = LoggerFactory.getLogger(HeatmapController.class);

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

        Random r = new Random();
        return r.nextDouble();
    }
}
