package com.hydra.web

import java.util.regex.Pattern
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import com.hwaipy.hydrogen.web.servlet.AsyncHttpServlet

object ClientDocumentServlet {
  val path = "/documents/*"
  val pathPattern = Pattern.compile("/documents/(.*)\\??.*")
}

class ClientDocumentServlet extends AsyncHttpServlet {

  override def doGetAsync(req: HttpServletRequest, resp: HttpServletResponse) {
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
