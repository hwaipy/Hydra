package com.hydra.web

import java.nio.file.{FileVisitOption, Files, LinkOption, Path}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import java.math.BigInteger
import java.nio.file.attribute.BasicFileAttributes
import com.hwaipy.hydrogen.web.servlet.AsyncHttpServlet
import java.security.MessageDigest
import scala.language.postfixOps
import scala.collection.JavaConverters._
import scala.collection.mutable

object HydraLocalServlet {
  val path = "/hydralocal/*"
}

class HydraLocalServlet(resourcePath: Path) extends AsyncHttpServlet {

  override def doGetAsync(req: HttpServletRequest, resp: HttpServletResponse) {
    req.getQueryString match {
      case "validate" => resp.getWriter.print("This is HydraLocal.")
      case "list" => {
        update
        val out = resources.values.map(v => v.toString).mkString("\n")
        resp.getOutputStream.write(out.getBytes("UTF-8"))
      }
      case _ => throw new RuntimeException("Wrong Query.")
    }
  }

  private val resources = new mutable.HashMap[String, FileEntry]()
  update

  private def update = {
    val paths = Files.walk(resourcePath, FileVisitOption.FOLLOW_LINKS).iterator.asScala.toList
      .filter(p => Files.isRegularFile(p))
    val newKeys = paths.map(p => p.toString)
    resources.keys.toList.foreach(key => if (!newKeys.contains(key)) {
      resources.remove(key)
    })
    paths.foreach(path => {
      val attributes = Files.readAttributes[BasicFileAttributes](path, classOf[BasicFileAttributes], LinkOption.NOFOLLOW_LINKS)
      val size = attributes.size
      val lastModified = attributes.lastModifiedTime.toMillis
      val oldEntryOption = resources.get(path.toString)
      if (oldEntryOption == None || oldEntryOption.get.lastModified < lastModified) {
        resources(path.toString) = new FileEntry(path, lastModified, size, calculateMD5(path))
      }
    })
  }

  class FileEntry(val path: Path, val lastModified: Long, val size: Long, val hash: String) {
    println(path.toString)
    override def toString: String = s"${path.toString}\n${lastModified}\n$size\n${hash}"
  }

  private def calculateMD5(path: Path) = {
    val bytes = Files.readAllBytes(path)
    val md = MessageDigest.getInstance("MD5")
    md.update(bytes)
    new BigInteger(1, md.digest).toString(16)
  }
}