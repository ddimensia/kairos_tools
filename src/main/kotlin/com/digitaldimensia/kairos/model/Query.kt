package com.digitaldimensia.kairos.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

data class Query(
    @JsonProperty("start_absolute") val startAbsolute: Long,
    val metrics: List<MetricQuery>,
    @JsonProperty("end_absolute") @JsonInclude(JsonInclude.Include.NON_NULL) val endAbsolute: Long? = null,
    val plugins: List<String> = listOf()
)

data class MetricQuery(
    val name: String,
    val tags: Map<String, List<String>> = mapOf(),
    @JsonInclude(JsonInclude.Include.NON_EMPTY) val aggregators: List<Aggregator> = listOf(),
    @JsonProperty("group_by") @JsonInclude(JsonInclude.Include.NON_EMPTY) val groupBy: List<Group> = listOf()
)

open class Aggregator(val name: String)
data class SaveAsAggregator(
    @JsonProperty("metric_name") val destMetricName: String
) : Aggregator("save_as")

open class Group(val name: String)
data class TagGroup(val tags: Set<String>) : Group("tag")