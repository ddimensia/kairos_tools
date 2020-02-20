package com.digitaldimensia.kairos

import com.digitaldimensia.kairos.model.MetricsCommandParams
import com.github.ajalt.clikt.output.TermUi
import com.github.kittinunf.result.Result
import com.github.kittinunf.fuel.httpDelete
import java.lang.Thread.sleep
import java.net.URLEncoder

class MetricsCleaner(params: MetricsCommandParams): MetricsCommand(params) {

    override fun processMetrics(metricNames: List<String>) {
        if (metricNames.isEmpty()) {
            TermUi.echo("No metrics matched filter ${metricPattern.pattern()}!")
        } else {
            TermUi.echo("Matched ${metricNames.size} metrics: {${metricNames.joinToString(",")}}")
            TermUi.confirm("Delete?", abort = true)

            metricNames.map {
                val encodedName = URLEncoder.encode(it, "UTF8").replace("+", "%20")
                val request = "$url/metric/$encodedName".httpDelete().timeoutRead(120000)
                if (dryRun) {
                    TermUi.echo("Request: $request")
                } else {
                    val (_, _, deleteResult) = request.response()
                    when (deleteResult) {
                        is Result.Success -> {
                            TermUi.echo("Deleted $it")
                        }
                        is Result.Failure -> {
                            TermUi.echo("Failed to delete $it, exiting.")
                            TermUi.echo("${deleteResult.error.message}")
                            return
                        }
                    }
                    sleep(200)
                }
            }
        }

        TermUi.echo("Finished!")
    }
}
