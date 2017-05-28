package com.hydra.web

import java.util.regex.Pattern
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

object ClientDocumentServlet {
  val path = "/documents/*"
  val pathPattern = Pattern.compile("/documents/(.*)\\??.*")
}

class ClientDocumentServlet extends HttpServlet {

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    val matcher = ClientDocumentServlet.pathPattern.matcher(req.getRequestURI)
    if (!matcher.find) throw new RuntimeException("Wrong URL")
    val name = matcher.group(1)
    val html = try {
      WydraApp.client.blockingInvoker(name).getDocument()
    } catch {
      case e: Throwable =>
        (<HTML>
          <HEAD>
            <TITLE>Document Error</TITLE>
          </HEAD> <BODY>Error in getting document of client
            {name}
          </BODY>
        </HTML>).toString
    }
    resp.getWriter().print(html)
  }
}