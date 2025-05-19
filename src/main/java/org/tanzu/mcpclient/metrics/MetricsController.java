package org.tanzu.mcpclient.metrics;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    public MetricsService.Metrics getMetrics(HttpServletRequest request) {
        String conversationId = request.getSession().getId();
        return metricsService.getMetrics(conversationId);
    }
}