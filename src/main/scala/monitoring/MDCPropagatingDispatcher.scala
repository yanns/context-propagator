package monitoring

import java.util.concurrent.TimeUnit

import akka.dispatch._
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Configurator for a MDC propagating dispatcher.
 *
 * To use it, configure play like this:
 * {{{
 * play {
 *   akka {
 *     actor {
 *       default-dispatcher = {
 *         type = "monitoring.MDCPropagatingDispatcherConfigurator"
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * Credits to James Roper for the [[https://github.com/jroper/thread-local-context-propagation/ initial implementation]]
 */
class MDCPropagatingDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new MDCPropagatingDispatcher(
    this,
    config.getString("id"),
    config.getInt("throughput"),
    FiniteDuration(config.getDuration("throughput-deadline-time", TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS),
    configureExecutor(),
    FiniteDuration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance
}

/**
 * A MDC propagating dispatcher.
 *
 * This dispatcher propagates the MDC current request context if it's set when it's executed.
 */
class MDCPropagatingDispatcher(_configurator: MessageDispatcherConfigurator,
                               id: String,
                               throughput: Int,
                               throughputDeadlineTime: Duration,
                               executorServiceFactoryProvider: ExecutorServiceFactoryProvider,
                               shutdownTimeout: FiniteDuration)
  extends Dispatcher(_configurator, id, throughput, throughputDeadlineTime, executorServiceFactoryProvider, shutdownTimeout ) {

  self =>

  override def execute(runnable: Runnable): Unit = {

    // capture the caller MDC
    val capturedId = MDC.requestId.get()
    println(s"captured ID: $capturedId")

    super.execute(new Runnable {

      def run() = {
        // backup the callee MDC context
        val calleeId = MDC.requestId.get()
        println(s"callee ID to restore: $calleeId")

        // Run the runnable with the captured context
        MDC.requestId.set(capturedId)

        try {
          runnable.run()
        } finally {
          // restore the callee MDC context
          MDC.requestId.set(calleeId)
        }
      }
    })
  }

  def prepare_deactivated(): ExecutionContext = new ExecutionContext {
    // capture the MDC
    val capturedId = MDC.requestId.get()
    println(s"captured ID: $capturedId")

    def execute(r: Runnable) = self.execute(new Runnable {
      def run() = {
        // backup the callee MDC context
        val calleeId = MDC.requestId.get()
        println(s"callee ID to restore: $calleeId")

        // Run the runnable with the captured context
        MDC.requestId.set(capturedId)

        try {
          r.run()
        } finally {
          // restore the callee MDC context
          MDC.requestId.set(calleeId)
        }
      }
    })
    def reportFailure(t: Throwable) = self.reportFailure(t)
  }


}
