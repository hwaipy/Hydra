package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import java.nio.ByteBuffer
import com.hydra.io._
import com.hydra.core._
import scala.concurrent.{ExecutionContext, Future}
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.AtomicInteger
import scala.util.{Failure, Success}

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def explore() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.explore())
  }

  def tutorial() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.tutorial())
  }

  val manager = new MessageSessionManager
  val service = new StatelessMessageService(manager)
  val executionContext = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  def hydramessage() = Action { implicit request: Request[AnyContent] => {
    val contentLength: Int = request.headers("Content-Length").toInt
    if (contentLength > 10000000) throw new IllegalArgumentException("To much data!")
    val contentType = request.headers("Content-Type")
    val cookies = request.cookies
    val token = cookies.get("HydraToken")
    val machineID = s"${
      request.headers.get("Remote-Address") match {
        case None => "NoRemoteAddress"
        case Some(ra) => ra
      }
    }-${
      request.headers.get("User-Agent") match {
        case None => "NoUserAgent"
        case Some(ua) => ua
      }
    }"
    val message = contentType.toLowerCase match {
      case "application/msgpack" => {
        val bytes = request.body.asRaw.get.asBytes().get
        val decoder = new MessageGenerator()
        decoder.feed(bytes.asByteBuffer)
        decoder.next
      }
      case _ => throw new UnsupportedOperationException
    }
    message match {
      case None => throw new IllegalArgumentException
      case Some(msg) => {
        val newToken = service.messageDispatch(msg, new StatelessSessionProperties(machineID, None))
        val future = service.fetchNewMessage(newToken)
        //        val messageOption = Await.result(future, 1 minute)
        //        messageOption.foreach(messageReceived)
        future onComplete {
          case Success(post) => println(post)
          case Failure(t) => println("An error has occurred: " + t.getMessage)
        }(executionContext)
        Ok(views.html.tutorial()).withCookies(new Cookie("HydraToken", newToken))
      }
    }
  }
  }
}
