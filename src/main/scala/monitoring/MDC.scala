package monitoring

object MDC {

  val requestId = new ThreadLocal[String]()

}
