package com.hydra.sydrajs

import scala.scalajs.js.annotation.JSExportTopLevel
import org.querki.jquery._
import org.scalajs.dom.ext.Ajax
import com.hydra.core._

object TutorialApp {
  def main(args: Array[String]): Unit = {
    $(() => setupUI())
  }

  def setupUI(): Unit = {
    $("""<button type="button">Click me!</button>""")
      .click(() => addClickedMessage())
      .appendTo($("body"))
    $("body").append("<p>Hello World</p>")
    $("#click-me-button").click(() => addClickedMessage())
  }

  @JSExportTopLevel("addClickedMessage")
  def addClickedMessage(): Unit = {
    $("body").append("<p>You clicked the button! YES!</p>")
    val url = "http://localhost/hydra/message"
    //    val client = MessageClient.create(null, "SydraJSClientTestService", new Object())


    //    Ajax.post(url).onComplete{ case xhr =>
    //      target.appendChild(
    //        pre(xhr.responseText).render
    //      )
    //    }
  }
}