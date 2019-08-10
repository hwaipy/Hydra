import java.io.PrintWriter
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import com.hydra.core.MessageGenerator
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.collection.JavaConverters._

class ParserMonitor(monitoringPath: Path, resultPath: Path, startTime: Date, stopTime: Date) {
  private val executor = Executors.newFixedThreadPool(8)
  private val executionContext = ExecutionContext.fromExecutor(executor)

  def perform = {
    val dataPairs = getDataPairs
    dataPairs.map(pair => Future[Unit] {
      try {
        parse(pair)
      }
      catch {
        case e: Throwable => println(s"Error in ${pair}: $e")
      }
    }(executionContext))
  }

  def performAndStop = {
    val futures = perform
    futures.foreach(future => Await.result(future, 1 hour))
    stop
    Runtime.getRuntime.exec(s"python3 scripts/SummaryShower.py ${resultPath}").waitFor
  }

  def stop = executor.shutdown()

  private def parse(dataPair: Tuple2[Path, Path]) = {
    val parser = new Parser(dataPair, resultPath)
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
  private val qberFile = dataPair._1
  private val channelFile = dataPair._2
  val qbers = new QBERs(loadMsgpackEntries(qberFile))
  private val channels = new Channels(loadMsgpackEntries(channelFile))
  performTimeMatch
  performEntryMatch
  val timeMatchedQBEREntries = qbers.entries.filter(e => e.relatedChannelEntryCount > 0)
  val powerOffsets = (timeMatchedQBEREntries.map(p => p.relatedPowers._1).min, timeMatchedQBEREntries.map(p => p.relatedPowers._2).min)

  def parse = {
    val resultInnerPath = resultPath.resolve(qberFile.getFileName.toString.slice(0, 15))
    Files.createDirectories(resultInnerPath)
    showCountChannelRelations(resultInnerPath.resolve("CountChannelRelations"))
    Files.copy(resultInnerPath.resolve("CountChannelRelations.png"), Paths.get(resultInnerPath.toString + ".png"), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING)
    val halfRatios = Range(0, 40).map(i => math.pow(1.1, i))
    val ratios = halfRatios.reverse.dropRight(1).map(a => 1 / a) ++ halfRatios
    showHOMandQBERs(List(1e-10) ++ Range(1, 81).toList.map(i => i / 100.0), ratios.toList, resultInnerPath.resolve("HOMandQBERs.csv"))
  }

  private def loadMsgpackEntries(path: Path) = {
    val data = Files.readAllBytes(path)
    val generator = new MessageGenerator()
    generator.feed(data)
    val entries = new ListBuffer[Map[String, Any]]()
    while (generator.remainingBytes > 0) {
      entries += generator.next.get.content
    }
    entries.toArray
  }

  private def performTimeMatch = {
    val syncPairs = qbers.channelMonitorSyncs.dropRight(1).zip(qbers.channelMonitorSyncs.drop(1)) zip channels.riseIndices.dropRight(1).zip(channels.riseIndices.drop(1))
    syncPairs.foreach(syncPair => {
      val qberSyncPair = syncPair._1
      val channelSyncPair = syncPair._2
      Range(channelSyncPair._1, channelSyncPair._2).foreach(i => channels.entries(i).tdcTime set (i - channelSyncPair._1).toDouble / (channelSyncPair._2 - channelSyncPair._1) * (qberSyncPair._2 - qberSyncPair._1) + qberSyncPair._1)
    })
  }

  private def performEntryMatch = {
    val channelSearchIndexStart = new AtomicInteger(0)
    qbers.entries.foreach(qberEntry => {
      val channelSearchIndex = new AtomicInteger(channelSearchIndexStart get)
      val break = new AtomicBoolean(false)
      while (channelSearchIndex.get() < channels.entries.size && !break.get) {
        val channelEntry = channels.entries(channelSearchIndex.get)
        if (channelEntry.tdcTime.get < qberEntry.tdcStart) {
          channelSearchIndex.incrementAndGet
          channelSearchIndexStart.incrementAndGet
        } else if (channelEntry.tdcTime.get < qberEntry.tdcStop) {
          qberEntry.appendRelatedChannelEntry(channelEntry)
          channelSearchIndex.incrementAndGet
        } else break set true
      }
    })
  }

  private def showCountChannelRelations(path: Path) = {
    val counts = timeMatchedQBEREntries.map(_.counts)
    val powers = timeMatchedQBEREntries.map(_.relatedPowers)
    val writer = new PrintWriter(path.toString + ".csv")
    writer.println("Count 1, Count 2, Power 1, Power 2")
    counts.zip(powers).foreach(z => writer.println(s"${z._1(0)}, ${z._1(1)}, ${z._2._1}, ${z._2._2}"))
    writer.close

    val process = Runtime.getRuntime.exec(s"python3 scripts/CountChannelRelationsShower.py ${path.toString}.csv ${path.toString}.png")
    process.waitFor
  }

  private def showHOMandQBERs(thresholds: List[Double], ratios: List[Double], path: Path) = {
    val homAndQberEntries = thresholds.map(threshold => ratios.map(ratio => new HOMandQBEREntry(threshold, ratio, timeMatchedQBEREntries.size, powerOffsets))).flatten.toArray
    timeMatchedQBEREntries.foreach(entry => {
      val relatedPowers = entry.relatedPowers
      homAndQberEntries.filter(_.ratioAcceptable(relatedPowers._1, relatedPowers._2)).foreach(hqe => hqe.append(entry))
    })
    val pw = new PrintWriter(path.toFile)
    pw.println(HOMandQBEREntry.HEAD)
    homAndQberEntries.foreach(e => pw.println(e.toLine))
    pw.close()
  }
}

class QBERs(val sections: Array[Map[String, Any]]) {
  val systemTimes = sections.map(section => section("Time"))
  val TDCTimeOfSectionStart = sections(0)("ChannelMonitorSync").asInstanceOf[List[Long]](0)
  val channelMonitorSyncs = sections.map(section => section("ChannelMonitorSync").asInstanceOf[List[Long]].drop(2)).flatten.map(s => (s - TDCTimeOfSectionStart) / 1e12)
  val entries = sections.map(section => {
    val HOMSections = section("HOMSections").asInstanceOf[List[List[Double]]].toArray
    val QBERSections = section("QBERSections").asInstanceOf[List[List[Int]]].toArray
    val countSections = section("CountSections").asInstanceOf[List[List[Int]]].toArray
    val tdcStartStop = section("ChannelMonitorSync").asInstanceOf[List[Long]].slice(0, 2).map(time => (time - TDCTimeOfSectionStart) / 1e12)
    val entryCount = countSections.size
    Range(0, entryCount).map(i => {
      val entryTDCStartStop = List(i, i + 1).map(j => (tdcStartStop(1) - tdcStartStop(0)) / entryCount * j + tdcStartStop(0))
      val entryHOMs = HOMSections.map(j => j(i))
      val entryQBERs = QBERSections(i).map(j => j)
      val entryCounts = countSections(i).map(j => j)
      new QBEREntry(entryTDCStartStop(0), entryTDCStartStop(1), entryHOMs, entryCounts.toArray, entryQBERs.toArray)
    })
  }).flatten
  validate()

  private def validate() = {
    val channelMonitorSyncZip = channelMonitorSyncs.dropRight(1).zip(channelMonitorSyncs.drop(1))
    val valid = channelMonitorSyncZip.map(z => math.abs(z._2 - z._1 - 10) < 0.001).forall(b => b)
    if (!valid) throw new IllegalArgumentException(s"Error in ChannelMonitorSyncs of QBER: ${channelMonitorSyncs}")
  }
}

class QBEREntry(val tdcStart: Double, val tdcStop: Double, val HOMs: Array[Double], val counts: Array[Int], val QBERs: Array[Int]) {
  private val relatedChannelEntryBuffer = new ArrayBuffer[ChannelEntry]()

  def appendRelatedChannelEntry(channelEntry: ChannelEntry) = relatedChannelEntryBuffer += channelEntry

  def relatedChannelEntryCount = relatedChannelEntryBuffer.size

  def relatedPowers =
    if (relatedChannelEntryCount == 0) (0.0, 0.0)
    else (relatedChannelEntryBuffer.map(e => e.power1).sum / relatedChannelEntryCount,
      relatedChannelEntryBuffer.map(e => e.power2).sum / relatedChannelEntryCount)
}

class Channels(val sections: Array[Map[String, Any]], val triggerThreshold: Double = 1.0) {
  val systemTimes = sections.map(section => section("SystemTime"))
  val entries = sections.map(section => {
    val channelDatas = section("Monitor").asInstanceOf[List[List[Double]]]
    channelDatas.map(channelData => new ChannelEntry(channelData.slice(0, 3), channelData(3)))
  }).flatten
  val riseIndices = entries.dropRight(1).zip(entries.drop(1)).zipWithIndex
    .map(z => (z._1._1.trigger, z._1._2.trigger, z._2)).filter(z => z._1 < triggerThreshold && z._2 > triggerThreshold).map(_._3)
  validate()

  private def validate() = {
    val risesZip = riseIndices.dropRight(1).zip(riseIndices.drop(1))
    val valid = risesZip.map(z => math.abs((entries(z._2).refTime - entries(z._1).refTime) / 1000 - 10) < 0.02).forall(b => b)
    if (!valid) throw new IllegalArgumentException(s"Error in ChannelMonitorSyncs of Channel: ${riseIndices.toList}")
  }
}

class ChannelEntry(val powers: List[Double], val refTime: Double) {
  val power1 = powers(1)
  val power2 = powers(2)
  val trigger = powers(0)
  val tdcTime = new AtomicReference[Double](-1)
}

object HOMandQBEREntry {
  private val bases = List("O", "X", "Y", "Z")
  val HEAD = s"Threshold, Ratio, ValidTime, TotalTime, " +
    (List("XX", "YY", "All").map(bb => List("Dip", "Act").map(position => bb + position)).flatten.mkString(", ")) + ", " +
    (bases.map(a => bases.map(b => List("Correct", "Wrong").map(cw => a + b + " " + cw))).flatten.flatten.mkString(", "))
}

class HOMandQBEREntry(val threshold: Double, val ratio: Double, val totalSectionCount: Int, val powerOffsets: Tuple2[Double, Double] = (0, 0), val powerInvalidLimit: Double = 4.5) {
  private val homCounts = new Array[Double](6)
  private val qberCounts = new Array[Int](32)
  private val validSectionCount = new AtomicInteger(0)

  def ratioAcceptable(rawPower1: Double, rawPower2: Double) = {
    val power1 = rawPower1 - powerOffsets._1
    val power2 = rawPower2 - powerOffsets._2
    val actualRatio = if (power2 == 0) 0 else power1 / power2 * ratio
    if (power1 > powerInvalidLimit || power2 > powerInvalidLimit) false
    else (actualRatio > threshold) && (actualRatio < (1 / threshold))
  }

  def append(qberEntry: QBEREntry) = {
    val homs = qberEntry.HOMs
    Range(0, 3).foreach(kk => {
      homCounts(kk * 2) += homs(kk * 2)
      homCounts(kk * 2 + 1) += homs(kk * 2 + 1)
    })
    val qbers = qberEntry.QBERs
    Range(0, qberCounts.size).foreach(kk => qberCounts(kk) += qbers(kk))
    validSectionCount.incrementAndGet
  }

  def toLine = s"$threshold, $ratio, ${validSectionCount.get / 1000.0}, ${totalSectionCount / 1000.0}, " + homCounts.mkString(", ") + ", " + qberCounts.mkString(", ")
}

object Parser extends App {
  val format = new SimpleDateFormat("yyyyMMdd-hhmmss.SSS")
  val startTime = format.parse("20190101-000000.000")
  val stopTime = format.parse("20191231-235959.999")
  //  val rootPath = Paths.get("E:\\MDIQKD_Parse\\RawData")
  val rootPath = Paths.get("/Users/hwaipy/Desktop/MDI")
  val availableDates = Files.list(rootPath).iterator.asScala.toList.filter(p => p.getFileName.toString.size == 8).sorted
  val availableRuns = availableDates.map(date => Files.list(date).iterator.asScala.toList.filter(p => p.getFileName.toString.toLowerCase.startsWith("run") && !p.getFileName.toString.toLowerCase.endsWith("parsed"))).flatten.sorted
  println(s"Parsing ${availableRuns.size} runs in ${availableDates.size} days.")

  availableRuns.foreach(runPath => {
    val monitor = new ParserMonitor(runPath, Paths.get(runPath.toString + "-parsed"), startTime, stopTime)
    monitor.performAndStop
  })
}