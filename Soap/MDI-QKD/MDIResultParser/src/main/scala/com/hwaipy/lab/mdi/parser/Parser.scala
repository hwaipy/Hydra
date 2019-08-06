import java.nio.file.{CopyOption, Files, Path, Paths, StandardCopyOption}
import java.text.{DateFormat, SimpleDateFormat}
import java.time.Duration
import java.util.Date

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters

object Parser extends App {
  val format = new SimpleDateFormat("yyyyMMdd-hhmmss.SSS")
  val startTime = format.parse("20190804-000000.000")
  val stopTime = format.parse("20190804-235900.000")
  val monitor = new ParserMonitor(Paths.get("C:\\Users\\Administrator\\Desktop\\Dumped"), Paths.get("C:\\Users\\Administrator\\Desktop\\ResultsScala"), startTime, stopTime)
  monitor.begin()
}

class ParserMonitor(monitoringPath:Path, resultPath:Path, startTime:Date, stopTime:Date){
  if (!Files.exists(resultPath.resolve("dumped"))) Files.createDirectories(resultPath.resolve("dumped"))
  if (!Files.exists(resultPath.resolve("results"))) Files.createDirectories(resultPath.resolve("results"))

  def begin() = {
    val dataPairs = getDataPairs
    dataPairs.slice(0, 1).foreach(parse)
  }

  private def parse(dataPair:Tuple2[Path, Path]) ={
    val parser = new Parser(dataPair, resultPath)
    parser.storeDumpedFiles
    parser.parse
  }
  private def getDataPairs={
    val entries = Files.list(monitoringPath).toArray.toList.asInstanceOf[List[Path]].sorted.filter(_.toString.toLowerCase.endsWith(".dump")).map(p => (p, Parser.format.parse (p.getFileName.toString.slice(0,19))))
    val inTimeEntries = entries.filter(e => e._2.after(startTime) && e._2.before(stopTime))
    val inTimeQBEREntries = inTimeEntries.filter(_._1.getFileName.toString.toLowerCase.endsWith("_qber.dump"))
    val inTimeChannelEntries = inTimeEntries.filter(_._1.getFileName.toString.toLowerCase.endsWith("_channel.dump"))
    if (inTimeQBEREntries.isEmpty || inTimeChannelEntries.isEmpty) Nil
    else{
      val buffer = new ArrayBuffer[Tuple2[Path, Path]]()
      val itQBERs = inTimeQBEREntries.iterator
      val itChannels = inTimeChannelEntries.iterator
      var QBEREntry = itQBERs.next
      var channelEntry = itChannels.next
      while (QBEREntry != null && channelEntry != null){
        val delta = Duration.between(channelEntry._2.toInstant, QBEREntry._2.toInstant).toMillis / 1000.0
        if (math.abs(delta) < 3){
          buffer += ((QBEREntry._1, channelEntry._1))
          QBEREntry = null
          channelEntry = null
        }else if (delta >0) channelEntry = null else QBEREntry = null
        if(QBEREntry == null && itQBERs.hasNext) QBEREntry = itQBERs.next
        if(channelEntry == null && itChannels.hasNext) channelEntry = itChannels.next
      }
      buffer.toList
    }
  }
}

class Parser(dataPair:Tuple2[Path, Path], resultPath :Path){

  def parse = {
    println("parse here")
  }

  def storeDumpedFiles = {
    Files.copy(dataPair._1, resultPath.resolve("dumped").resolve(dataPair._1.getFileName), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    Files.copy(dataPair._2, resultPath.resolve("dumped").resolve(dataPair._2.getFileName), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
  }
}