package com.credera.demo.opentracing.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class CrossSectionController {
    private static final Logger LOG = LoggerFactory.getLogger(CrossSectionController.class);

    @RequestMapping("/section")
    public double[] crossSection(
            @RequestParam(value = "minX") Integer minX,
            @RequestParam(value = "minY") Integer minY,
            @RequestParam(value = "maxX") Integer maxX,
            @RequestParam(value = "maxY") Integer maxY) {
        Random r = new Random();
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

        int len = (int) Math.sqrt(Math.pow(maxX - minX, 2.0) + Math.pow(maxY - minY, 2.0));
        double[] histogram = new double[len];
        for (int i = 0; i < len; i++) {
            histogram[i] = r.nextDouble();
        }

        return histogram;
    }
}

