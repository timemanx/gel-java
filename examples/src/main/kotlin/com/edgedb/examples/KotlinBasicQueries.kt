package com.edgedb.examples

import com.edgedb.driver.EdgeDBClient
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory

class KotlinBasicQueries : KotlinExample {
    companion object {
        private val logger = LoggerFactory.getLogger(KotlinBasicQueries::class.java)!!
    }

    override suspend fun runAsync(client: EdgeDBClient) {
        // the 'query' method enforces the cardinality of 'MANY', meaning zero OR X elements are
        // returned. This translates to a collection of nullable results.
        val queryResult = client.query(String::class.java, "SELECT 'Hello, Kotlin!'").await()
        logger.info("'query' method result: {}", queryResult)

        // 'querySingle' enforces the cardinality 'AT_MOST_ONE', meaning zero OR one element must
        // be returned from the query. This translates to a nullable result
        val querySingleResult = client.querySingle(String::class.java, "SELECT 'Hello, Kotlin!'").await()
        logger.info("'querySingle' method result: {}", querySingleResult)

        // 'queryRequiredSingle' enforces the cardinality 'ONE', meaning one element must be returned.
        // This translates to a non-null result.
        val queryRequiredSingleResult = client.queryRequiredSingle(
                String::class.java,
                "SELECT 'Hello, Kotlin!'"
        ).await()
        logger.info("'queryRequiredSingle' method result: {}", queryRequiredSingleResult)

        // 'execute' disregards the result entirely.
        client.execute("SELECT 'Hello, Kotlin!'").await()
    }
}