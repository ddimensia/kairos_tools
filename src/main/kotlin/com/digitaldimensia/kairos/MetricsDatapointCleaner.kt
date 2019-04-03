package com.digitaldimensia.kairos

import com.digitaldimensia.kairos.model.MetricQuery
import com.digitaldimensia.kairos.model.MetricsCommandParams
import com.digitaldimensia.kairos.model.Query
import com.digitaldimensia.kairos.model.QueryTagsResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.ajalt.clikt.output.TermUi
import com.github.kittinunf.result.Result
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.jackson.responseObject

class MetricsDatapointCleaner(
    params: MetricsCommandParams,
    private val tag: String,
    private val retainedValues: List<String>): MetricsCommand(params) {
    private val mapper = ObjectMapper().registerKotlinModule()

    override fun processMetrics(metricNames: List<String>) {
        if (metricNames.isEmpty()) {
            TermUi.echo("No metrics matched filter ${metricPattern.pattern()}!")
        } else {
            TermUi.echo("Matched ${metricNames.size} metrics: {${metricNames.joinToString(",")}}")
            TermUi.confirm("Continue?", abort = true)

            metricNames.map { metricName ->
                TermUi.echo("Processing metric '$metricName'")
                val query =
                    Query(0, listOf(MetricQuery(metricName)))
                val request = "$url/datapoints/query/tags".httpPost()
                    .header("Content-Type", "application/json")
                    .body(mapper.writeValueAsString(query))
                val (_, _, result) = request.responseObject<QueryTagsResult>()
                val tagResult = when (result) {
                    is Result.Success -> {
                        result.value.queries.first().results.first()
                    }
                    is Result.Failure -> {
                        TermUi.echo("Failed to get tags for $metricName, exiting: $result")
                        return
                    }
                }

                if (tagResult.tags[tag]?.intersect(retainedValues).orEmpty().isNotEmpty()) {
                    val badValues = tagResult.tags[tag]?.filterNot { retainedValues.contains(it) }.orEmpty()
                    badValues.map { badValue ->
                        val deleteQuery = Query(
                            0,
                            listOf(MetricQuery(metricName, mapOf(tag to listOf(badValue))))
                        )
                        val deleteRequest = "$url/datapoints/delete".httpPost()
                            .header("Content-Type", "application/json")
                            .body(mapper.writeValueAsString(deleteQuery))
                        if (dryRun) {
                            TermUi.echo(deleteRequest)
                        } else {
                            val (_, _, deleteResult) = deleteRequest.response()
                            when (deleteResult) {
                                is Result.Success -> {
                                    TermUi.echo("Deleted value '$badValue' from '$metricName'")
                                }
                                is Result.Failure -> {
                                    TermUi.echo("Failed to delete '$badValue' from '$metricName', exiting: $deleteResult")
                                    return
                                }
                            }
                        }
                    }
                } else {
                    TermUi.echo("No bad tag values for '$metricName'")
                }
            }
        }
    }
}
