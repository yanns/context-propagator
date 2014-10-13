package app


import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import monitoring.MDC

import scala.concurrent.duration._
import scala.concurrent.{Promise, Future}

object TestApp extends App {

  val system = ActorSystem("mdc", ConfigFactory.load())
  implicit val executionContext = system.dispatcher

  def mockFuture: Future[String] = {
    val result = Promise[String]()
    system.scheduler.scheduleOnce(1.second) {
      result.success("hello")
    }
    result.future
  }


  MDC.requestId.set("request1")

  println(s"sync request Id should be 1: ${MDC.requestId.get()}")
  mockFuture.map { s =>
    println(s"async request Id should be 1: ${MDC.requestId.get()}")
  }
  println(s"sync request Id should be 1: ${MDC.requestId.get()}")

  MDC.requestId.set("request2")

  println(s"sync request Id should be 2: ${MDC.requestId.get()}")
  mockFuture.map { s =>
    println(s"async request Id should be 2: ${MDC.requestId.get()}")
  }
  println(s"sync request Id should be 2: ${MDC.requestId.get()}")

  system.shutdown()
}
