package com.hydra.hap

import java.io.File
import org.pegdown.PegDownProcessor

import scala.io.Source

class SydraAppHandler(clientName: String, descriptionFile: String) {
  def getSummary() = {
    (<html>
      {clientName}
    </html>).toString
  }

  def getDocument() = {
    val file = new File(descriptionFile)
    file.exists match {
      case false => "Document does not exists."
      case true => try {
        val content = Source.fromFile(file).getLines.mkString("\n")
        descriptionFile.toLowerCase.split("\\.").last match {
          case "md" => (<html>
            <head>
              <title>Document:
                {clientName}
              </title>
            </head> <body>#1</body>
          </html>).toString.replace("#1", new PegDownProcessor().markdownToHtml(content))
          case _ => content
        }
      } catch {
        case e: Throwable => s"Description Error\n${e}"
      }
    }
  }
}