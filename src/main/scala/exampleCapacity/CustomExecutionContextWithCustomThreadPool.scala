import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object CustomExecutionContextWithCustomThreadPool {
  val customThreadPoolSize#: Int = 5000
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(customThreadPoolSize#))
}
