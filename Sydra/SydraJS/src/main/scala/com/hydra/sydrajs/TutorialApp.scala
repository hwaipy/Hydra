package com.hydra.sydrajs

import scala.scalajs.js.annotation.JSExportTopLevel
import org.querki.jquery._
import org.scalajs.dom.ext.Ajax

object TutorialApp {
  def main(args: Array[String]): Unit = {
    $(() => setupUI())
  }

  def setupUI(): Unit = {
    $("body").append("<p>Hello World</p>")
    $("#click-me-button").click(() => addClickedMessage())
  }

  @JSExportTopLevel("addClickedMessage")
  def addClickedMessage(): Unit = {
    $("body").append("<p>You clicked the button! YES!</p>")



    val url = "http://localhost/hydra/message"
//    Ajax.post(url).onComplete{ case xhr =>
//      target.appendChild(
//        pre(xhr.responseText).render
//      )
//    }
  }
}