package no.nav.syfo.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "pale_2_regler"

val HTTP_HISTOGRAM: Histogram = Histogram.Builder()
        .namespace(METRICS_NS)
        .labelNames("path")
        .name("requests_duration_seconds")
        .help("http requests durations for incoming requests in seconds")
        .register()

val RULE_HIT_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("rule_hit_counter")
        .labelNames("rule_name")
        .help("Registers a counter for each rule in the rule set")
        .register()


val RULE_HIT_STATUS_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("rule_hit_status_counter")
        .labelNames("rule_status")
        .help("Registers a counter for each rule status")
        .register()