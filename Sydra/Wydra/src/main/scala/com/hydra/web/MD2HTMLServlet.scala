package com.hydra.web

import java.io.DataInputStream
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import com.hwaipy.hydrogen.web.servlet.AsyncHttpServlet
import org.pegdown.PegDownProcessor
import scala.io.Source
import scala.language.postfixOps

object MD2HTMLServlet {
  val path = "/md2html/*"
}

class MD2HTMLServlet extends AsyncHttpServlet {

  override def doPostAsync(req: HttpServletRequest, resp: HttpServletResponse) {
    println("ahaha")
    val buffer = new Array[Byte](req.getContentLength)
    new DataInputStream(req.getInputStream()).readFully(buffer)
    try {
      val content = Source.fromBytes(buffer, "UTF-8").getLines.mkString("\n")
      val md = new PegDownProcessor().markdownToHtml(content)
      val html = (<html>
        <head>
        </head> <body>#1</body>
      </html>
        ).toString.replace("#1", md)
      resp.getOutputStream.write(html.getBytes("UTF-8"))
    } catch {
      case e: Throwable => println(e)
    }

  }
}