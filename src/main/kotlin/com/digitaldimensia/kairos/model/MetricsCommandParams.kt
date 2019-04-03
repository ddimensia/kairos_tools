package com.digitaldimensia.kairos.model

data class MetricsCommandParams(
    var url: String = "http://localhost:8080",
    var dryRun: Boolean = true,
    var metricNameRegex: String = ".*"
)
