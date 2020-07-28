import example.package.mixin.ExampleCustomExecutionContext

import scala.concurrent.Future

object ExampleRetryABlockOfCodeXTimes extends ExampleCustomExecutionContext {
  def apply[A](xAttempts: Int, exampleErrorHandler: PartialFunction[Throwable, Unit])(blockOfCode: => Future[A]): Future[A] = {
    def loop(remainingXAttempts: Int): Future[A] = blockOfCode recoverWith {
      case _ if remainingXAttempts > 1 =>
        loop(remainingXAttempts - 1)
      case e =>
        exampleErrorHandler(e)
        Future.failed(e)
    }
    loop(xAttempts)
  }
}

// example uses:

val exampleNumberOfRetriesOrAttempts = 5

Retry(exampleNumberOfRetriesOrAttempts, exampleErrorHandler("example-error-message")) {
// block of code to retry
// if it throws an exception or returns a Future.failed() it will be retried if there are more attempts remaining
}

// The exampleErrorHandler function is defined separately by each class/object/thing which is using the ExampleyRetryABlockOfCodeXTimes so that it can pass custom error messages and increment specific stats. Also StatsD seems to be difficult/impossible to mock.

// Example errorHandler function defined in one of the scala classes which is using it.

private def exampleErrorHandler(extraInfo: String): PartialFunction[Throwable, Unit] = {
case e =>
// StatsD
increment(s"error-retries-$statsKey-$extraInfo", s"error.retries.$statsKey.$extraInfo")
val logMessage = s"$extraInfo failed after retrying $daoRetries times with exception [${e.getMessage}]"
log.error(logMessage)
Future.failed(new Exception())
}

// Another example:

private def exampleErrorHandler[T](method: String, extralInfo: String): PartialFunction[Throwable, Future[T]] = {
case e =>
log.error("example error " + extralInfo)
Future.failed(new Exception())
}

// Another example:

private def exampleErrorHandler(response: Response, extralInfo: String) = {
log.error(s"example error ${response.getStatusCode()}" + extralInfo)
Future.failed(new Exception())
}



