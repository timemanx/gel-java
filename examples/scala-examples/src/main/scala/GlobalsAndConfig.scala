package com.geldata.examples

import com.geldata.driver.GelClientPool
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer
import com.geldata.driver.state.Config
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.jdk.FutureConverters.*

class GlobalsAndConfig extends Example:
  private val logger = LoggerFactory.getLogger(classOf[GlobalsAndConfig])

  override def run(clientPool: GelClientPool)(implicit context: ExecutionContext): Future[Unit] = {
    val configuredClient = client.withConfig(Config.builder()
      .withQueryExecutionTimeout(Duration.ZERO)
      .applyAccessPolicies(true)
      .build()
    ).withGlobals(
      Map[String, Object](
        "current_user_id" -> UUID.randomUUID()
      ).asJava
    )

    for(
      result <- configuredClient.queryRequiredSingle(
        classOf[UUID],
        "SELECT GLOBAL current_user_id"
      ).asScala
    ) yield {
      logger.info("Current user ID: {}", result)
    }
  }
