package com.digitaldimensia.kairos

import com.digitaldimensia.kairos.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.ajalt.clikt.output.TermUi
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.Result
import java.time.Instant

class MetricsCopier(
    params: MetricsCommandParams,
    private val replacement: String,
    private val startDate: Instant,
    private val endDate: Instant): MetricsCommand(params) {

    private val mapper = ObjectMapper().registerKotlinModule()

    override fun processMetrics(metricNames: List<String>) {
        val nameMap = metricNames.associateWith { metricPattern.matcher(it).replaceAll(replacement) }
        TermUi.echo("Matched ${metricNames.size} metrics")
        TermUi.echo("Renaming:")
        nameMap.forEach {
            TermUi.echo("\t${it.key} -> ${it.value}")
        }

        TermUi.confirm("Rename?", abort = true)

        nameMap.forEach { (srcMetric, destMetric) ->
            TermUi.echo("Processing metric '$srcMetric'")
            val query =
                Query(
                    startAbsolute = startDate.toEpochMilli(),
                    metrics = listOf(MetricQuery(srcMetric)),
                    endAbsolute = endDate.toEpochMilli()
                )
            val request = "$url/datapoints/query/tags".httpPost()
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(query))
            val (_, _, result) = request.responseObject<QueryTagsResult>()
            val tagResult = when (result) {
                is Result.Success -> {
                    result.value.queries.first().results.first()
                }
                is Result.Failure -> {
                    TermUi.echo("Failed to get tags for $srcMetric, exiting: $result")
                    return
                }
            }

            val saveAsQuery = Query(
                startAbsolute = startDate.toEpochMilli(),
                metrics = listOf(
                    MetricQuery(
                        name = srcMetric,
                        aggregators = listOf(SaveAsAggregator(destMetric)),
                        groupBy = listOf(TagGroup(tagResult.tags.keys))
                    )
                ),
                endAbsolute = endDate.toEpochMilli()
            )

            val saveAsRequest = "$url/datapoints/query".httpPost()
                .header("Content-Type", "application/json")
                .body(mapper.writeValueAsString(saveAsQuery))
            if (dryRun) {
                TermUi.echo("Request: $saveAsRequest")
            } else {
                val (_, _, saveAsResult) = saveAsRequest.responseString()
                when (saveAsResult) {
                    is Result.Success -> {
                        TermUi.echo("Copied $srcMetric to $destMetric")
                    }
                    is Result.Failure -> {
                        TermUi.echo("Failed to copy $srcMetric to $destMetric, exiting: $saveAsResult")
                        return
                    }
                }
            }
        }
    }
}
