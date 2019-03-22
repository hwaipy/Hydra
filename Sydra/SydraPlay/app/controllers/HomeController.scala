package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.http._
import java.nio.ByteBuffer
import com.hydra.io._
import com.hydra.core._
import scala.concurrent.{ExecutionContext, Future}
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.{Executors, ThreadFactory}
import java.util.concurrent.atomic.AtomicInteger
import scala.util.{Failure, Success}
import akka.util.ByteString

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

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

  def hydramessage() = Action.async { implicit request: Request[AnyContent] => {
    try {
      val contentLength: Int = request.headers("Content-Length").toInt
      if (contentLength > 10000000) throw new IllegalArgumentException("To much data!")
      val contentType = request.headers("Content-Type")
      val cookies = request.cookies
      val token = cookies.get("HydraToken").map(_.value)
      val machineID = s"${
        request.headers.get("Remote-Address") match {
          case None => "NoRemoteAddress"
          case Some(ra) => ra.substring(0, ra.lastIndexOf(":"))
        }
      }-${
        request.headers.get("User-Agent") match {
          case None => "NoUserAgent"
          case Some(ua) => ua
        }
      }"
      val newToken = contentLength match {
        case l if l > 0 => {
          val message = {
            contentType.toLowerCase match {
              case "application/msgpack" => {
                val bytes = request.body.asRaw.get.asBytes().get
                val decoder = new MessageGenerator()
                decoder.feed(bytes.asByteBuffer)
                decoder.next
              }
              case _ => throw new UnsupportedOperationException
            }
          } match {
            case None => throw new IllegalArgumentException
            case Some(msg) => msg
          }
          service.messageDispatch(message, new StatelessSessionProperties(machineID, token))
        }
        case _ => token.get
      }
      val future = service.fetchNewMessage(newToken)
      val futureResult: Future[Result] = future.map {
        messageOption => {
          val content = messageOption match {
            case None => new Array[Byte](0)
            case Some(responseMessage) => contentType.toLowerCase match {
              case "application/msgpack" => {
                val encoder = new MessagePacker()
                encoder.feed(responseMessage).pack()
              }
              case _ => throw new UnsupportedOperationException
            }
          }
          val result = Result(
            header = ResponseHeader(200),
            body = HttpEntity.Strict(ByteString(content), Some("application/msgpack"))
          )
          result.withCookies(new Cookie("HydraToken", newToken))
        }
      }
      futureResult
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        null
      }
    }
  }
  }
}
