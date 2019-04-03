package com.digitaldimensia.kairos.model

data class QueryTagsResult(val queries: List<QueryResult>)

data class QueryResult(val results: List<MetricTagsResult> = listOf())

data class MetricTagsResult(val name: String, val tags: Map<String, List<String>>)