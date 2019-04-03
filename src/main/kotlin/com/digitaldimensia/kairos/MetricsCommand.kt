package com.digitaldimensia.kairos

import com.digitaldimensia.kairos.model.MetricNamesResponse
import com.digitaldimensia.kairos.model.MetricsCommandParams
import com.github.ajalt.clikt.output.TermUi
import com.github.kittinunf.result.Result
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import java.util.regex.Pattern

abstract class MetricsCommand(params: MetricsCommandParams) {
    protected val url: String = params.url
    protected val metricPattern: Pattern = Pattern.compile(params.metricNameRegex)
    protected val dryRun: Boolean = params.dryRun

    fun run() {
        val (_, _, result) = Fuel.get("$url/metricnames")
            .responseObject<MetricNamesResponse>()

        val metricNames = when(result) {
            is Result.Success -> {
                TermUi.echo("Found ${result.value.results.size} total metric names")
                result.value
            }
            is Result.Failure -> {
                TermUi.echo("Failed to get metric names ${result.error}")
                return
            }
        }

        processMetrics(metricNames.results.filter {metricPattern.matcher(it).matches()})
    }

    abstract fun processMetrics(metricNames: List<String>)
}