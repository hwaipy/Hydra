import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import com.hydra.core.MessageGenerator
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class ParserMonitor(monitoringPath: Path, resultPath: Path, startTime: Date, stopTime: Date) {
  if (!Files.exists(resultPath.resolve("dumped"))) Files.createDirectories(resultPath.resolve("dumped"))
  if (!Files.exists(resultPath.resolve("results"))) Files.createDirectories(resultPath.resolve("results"))

  def begin() = {
    val dataPairs = getDataPairs
    dataPairs.slice(0, 1).foreach(parse)
  }

  private def parse(dataPair: Tuple2[Path, Path]) = {
    val parser = new Parser(dataPair, resultPath)
    parser.storeDumpedFiles
    parser.parse
  }

  private def getDataPairs = {
    val entries = Files.list(monitoringPath).toArray.toList.asInstanceOf[List[Path]].sorted.filter(_.toString.toLowerCase.endsWith(".dump")).map(p => (p, Parser.format.parse(p.getFileName.toString.slice(0, 19))))
    val inTimeEntries = entries.filter(e => e._2.after(startTime) && e._2.before(stopTime))
    val inTimeQBEREntries = inTimeEntries.filter(_._1.getFileName.toString.toLowerCase.endsWith("_qber.dump"))
    val inTimeChannelEntries = inTimeEntries.filter(_._1.getFileName.toString.toLowerCase.endsWith("_channel.dump"))
    if (inTimeQBEREntries.isEmpty || inTimeChannelEntries.isEmpty) Nil
    else {
      val buffer = new ArrayBuffer[Tuple2[Path, Path]]()
      val itQBERs = inTimeQBEREntries.iterator
      val itChannels = inTimeChannelEntries.iterator
      var QBEREntry = itQBERs.next
      var channelEntry = itChannels.next
      while (QBEREntry != null && channelEntry != null) {
        val delta = Duration.between(channelEntry._2.toInstant, QBEREntry._2.toInstant).toMillis / 1000.0
        if (math.abs(delta) < 3) {
          buffer += ((QBEREntry._1, channelEntry._1))
          QBEREntry = null
          channelEntry = null
        } else if (delta > 0) channelEntry = null else QBEREntry = null
        if (QBEREntry == null && itQBERs.hasNext) QBEREntry = itQBERs.next
        if (channelEntry == null && itChannels.hasNext) channelEntry = itChannels.next
      }
      buffer.toList
    }
  }
}

class Parser(dataPair: Tuple2[Path, Path], resultPath: Path) {
  private val QBERFile = dataPair._1
  private val channelFile = dataPair._2
  private val qbers = new QBERs(loadMsgpackEntries(QBERFile))
  private val channels = new Channels(loadMsgpackEntries(channelFile))

  def parse = {
    //loadQBERs(QBERFile)
  }

  def storeDumpedFiles = {
    Files.copy(dataPair._1, resultPath.resolve("dumped").resolve(dataPair._1.getFileName), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    Files.copy(dataPair._2, resultPath.resolve("dumped").resolve(dataPair._2.getFileName), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
  }

  private def loadMsgpackEntries(path: Path) = {
    val data = Files.readAllBytes(path)
    val generator = new MessageGenerator()
    generator.feed(data)
    val entries = new ListBuffer[Map[String, Any]]()
    while (generator.remainingBytes > 0) {
      entries += generator.next.get.content
    }
    entries.toList
  }
}

class QBERs(val sections: List[Map[String, Any]]) {
  val systemTimes = sections.map(section => section("Time"))
  val TDCTimeOfSectionStart = sections(0)("ChannelMonitorSync").asInstanceOf[List[Long]](0)
  val channelMonitorSyncs = sections.map(section => section("ChannelMonitorSync").asInstanceOf[List[Long]].drop(2)).flatten.map(s => (s - TDCTimeOfSectionStart) / 1e12)
  val entries = sections.map(section => {
    val HOMSections = section("HOMSections").asInstanceOf[List[List[Double]]]
    val QBERSections = section("QBERSections").asInstanceOf[List[List[Int]]]
    val countSections = section("CountSections").asInstanceOf[List[List[Int]]]
    val tdcStartStop = section("ChannelMonitorSync").asInstanceOf[List[Long]].slice(0, 2).map(time => (time - TDCTimeOfSectionStart) / 1e12)
    val entryCount = countSections.size
    Range(0, entryCount).map(i => {
      val entryTDCStartStop = List(i, i + 1).map(j => (tdcStartStop(1) - tdcStartStop(0)) / entryCount * j + tdcStartStop(0))
      val entryHOMs = HOMSections.map(j => j(i))
      val entryQBERs = QBERSections(i).map(j => j)
      val entryCounts = countSections(i).map(j => j)
      new QBEREntry(entryTDCStartStop(0), entryTDCStartStop(1), entryHOMs, entryCounts, entryQBERs)
    })
  }).flatten
  validate()

  private def validate() = {
    val channelMonitorSyncZip = channelMonitorSyncs.dropRight(1).zip(channelMonitorSyncs.drop(1))
    val valid = channelMonitorSyncZip.map(z => math.abs(z._2 - z._1 - 10) < 0.001).forall(b => b)
    if (!valid) throw new IllegalArgumentException(s"Error in ChannelMonitorSyncs of QBER: ${channelMonitorSyncs}")
  }
}

class QBEREntry(val tdcStart: Double, val tdcStop: Double, val HOMs: List[Double], val counts: List[Int], val QBERs: List[Int]) {

  /*
        self.relatedChannelEntries = []

    def powerMatched(self, threshold, ratio, singleMatch=None):
        if len(self.relatedChannelEntries) == 0: return False
        powers = self.relatedPowers()
        if singleMatch is not None:
            powers[1] = singleMatch
        actualRatio = 0 if powers[1] == 0 else powers[0] / powers[1] * ratio
        if (powers[0]>4.5) or (powers[1]>4.5): return False
        return (actualRatio > threshold) and (actualRatio < (1 / threshold))

    def countMatched(self, threshold, ratio):
        if self.counts[0] * self.counts[1] == 0: return False
        actualRatio = 0 if self.counts[1] == 0 else self.counts[0] * 1.0 / self.counts[1] * ratio
        return (actualRatio > threshold) and (actualRatio < (1 / threshold))

    def relatedPowers(self):
        if len(self.relatedChannelEntries) == 0: return [0, 0]
        power1 = sum([c.power1 for c in self.relatedChannelEntries]) / len(self.relatedChannelEntries)
        power2 = sum([c.power2 for c in self.relatedChannelEntries]) / len(self.relatedChannelEntries)
        return [power1, power2]
   */
}

class Channels(val sections: List[Map[String, Any]], val triggerThreshold: Double = 1.0) {
  val systemTimes = sections.map(section => section("SystemTime"))
  val entries = sections.map(section => {
    val channelDatas = section("Monitor").asInstanceOf[List[List[Double]]]
    channelDatas.map(channelData => new ChannelEntry(channelData.slice(0, 3), channelData(3)))
  }).flatten
  val riseIndices = entries.drop(1).zip(entries.dropRight(1)).zipWithIndex
    .map(z => (z._1._1.trigger, z._1._2.trigger, z._2)).filter(z => z._1 < triggerThreshold && z._2 > triggerThreshold).map(_._3)
  validate()

  private def validate() = {
    val risesZip = riseIndices.dropRight(1).zip(riseIndices.drop(1))
    val valid = risesZip.map(z => math.abs((entries(z._2).refTime - entries(z._1).refTime) / 1000 - 10) < 0.02).forall(b => b)
    if (!valid) throw new IllegalArgumentException(s"Error in ChannelMonitorSyncs of Channel: ${riseIndices}")
  }
}

class ChannelEntry(val powers: List[Double], val refTime: Double) {
  val power1 = powers(1)
  val power2 = powers(2)
  val trigger = powers(0)
//self.tdcTime = -1
}


object Parser extends App {
  val baseDataPath = args.size match {
    case size if size > 0 => args(0)
    case 0 => "/Users/Hwaipy/Desktop/MDIProgramTest"
  }
  val format = new SimpleDateFormat("yyyyMMdd-hhmmss.SSS")
  val startTime = format.parse("20190804-000000.000")
  val stopTime = format.parse("20190804-235900.000")
  val monitor = new ParserMonitor(Paths.get(s"${baseDataPath}/Dumped"), Paths.get(s"${baseDataPath}/ResultsScala"), startTime, stopTime)
  monitor.begin()
}