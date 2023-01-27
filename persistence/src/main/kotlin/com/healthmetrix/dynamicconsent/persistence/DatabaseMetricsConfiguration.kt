package com.healthmetrix.dynamicconsent.persistence

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.annotation.PostConstruct
import javax.sql.DataSource

@Configuration
@Profile("postgres")
class DatabaseMetricsConfiguration(
    private val registry: MeterRegistry,
    private val dataSource: DataSource,
) {

    @PostConstruct
    fun initializeTableSizeMetrics() {
        listOf("consent_flows", "signed_consents", "cached_consents").forEach { tableName ->
            DatabaseTableMetrics.monitor(registry, tableName, "dynamic-consent", dataSource)
        }
    }
}
