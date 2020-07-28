

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

import scala.concurrent.duration._

object Launcher extends App {
  import CustomExecutionContextWithCustomThreadPool.executionContext

  val port = sys.props.get("jetty.port") map (_.toInt) getOrElse 8080

  val server = new Server(port)
  val context = new WebAppContext

  context.setContextPath("/")
  context.setResourceBase("src/main/webapp")
  context.addEventListener(new ScalatraListener)
  context.addServlet(classOf[DefaultServlet], "/")

  implicit val system = ServiceActorSystem.actorSystem

  system.scheduler.schedule(initialDelay = 0.seconds, interval = 1L.microseconds)({
    SomeService.someProcessingMethod()
  })

  server.setHandler(context)
  server.start()
  server.join()
}

