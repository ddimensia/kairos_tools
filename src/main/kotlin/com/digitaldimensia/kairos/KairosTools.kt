package com.digitaldimensia.kairos

import com.digitaldimensia.kairos.model.MetricsCommandParams
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import java.time.Instant

fun main(args: Array<String>) {
    KairosTools().subcommands(DeleteMetrics(), DeleteTagValues(), CopyMetrics()).main(args)
}

class KairosTools : CliktCommand() {
    private val hostname: String by option(
        "--host",
        help = "Kairosdb hostname (default: localhost)",
        metavar = "<hostname>"
    ).default("localhost")
    private val port: Int by option(help = "Kairosdb port (default: 8080)", metavar = "<port>").int().default(8080)
    private val metricNameRegex: String
            by option("--metricName", help = "Regex pattern for matching metric names", metavar = "<regex>").required()
    private val dryRun: Boolean by option(help = "Don't actually do anything").flag()
    private val config by findObject { MetricsCommandParams() }

    override fun run() {
        config.url = "http://$hostname:$port/api/v1"
        config.dryRun = dryRun
        config.metricNameRegex = metricNameRegex
    }
}

class DeleteMetrics : CliktCommand(name = "deleteMetrics", help = "Delete metric series") {
    private val config by requireObject<MetricsCommandParams>()
    override fun run() {
        MetricsCleaner(config).run()
    }
}

class DeleteTagValues : CliktCommand(name = "deleteTagValues", help = "Delete datapoints associate with tag values") {
    private val tagName: String by option("-t", "--tag", help = "Tag name").required()
    private val valuesToKeep: List<String> by option("-v", "--value", help = "Tag values to retain").multiple()
    private val config by requireObject<MetricsCommandParams>()
    override fun run() {
        MetricsDatapointCleaner(config, tagName, valuesToKeep).run()
    }
}

class CopyMetrics : CliktCommand(name = "copyMetrics", help = "Copy datapoints to a new metric name") {
    private val replacment: String by option("-r", help = "Sed Replacment value").required()
    private val start: Instant by option("-s", "--start", help = "Start date (e.g. '2019-01-01T00:00:00Z'")
        .convert("ISO-DATETIME") { Instant.parse(it) }
        .default(Instant.EPOCH)
    private val end: Instant by option("-e", "--end", help = "End date")
        .convert("ISO-DATETIME") { Instant.parse(it) }
        .default(Instant.now())
    private val config by requireObject<MetricsCommandParams>()
    override fun run() {
        MetricsCopier(config, replacment, start, end).run()
    }
}
