package com.mikrotasarim.ui.controller

import javafx.embed.swing.SwingFXUtils

import com.mikrotasarim.api.command.ApiConstants.{NucMode, TriggerMode, ResistanceMeasurementMode}
import com.mikrotasarim.ui.model.{Frame, Measurement}

import scala.util.Random
import scalafx.beans.property.{ObjectProperty, BooleanProperty, StringProperty}
import scalafx.collections.ObservableBuffer

import javafx.scene.image.Image

import scalafx.scene.chart.XYChart

object MeasurementController {

  def measureNetd(): Unit = {
    def netd(m0: Double, m1: Double, u0: Double, u1: Double, t0: Double, t1: Double): Seq[Double] =
      Seq(u0 / math.abs(m0 - m1) * math.abs(t0 - t1), u1 / math.abs(m0 - m1) * math.abs(t0 - t1))

    val netds = (for (i <- 0 until 384 * 288) yield
      netd(
        measurement.netdMeansT0(i),
        measurement.netdMeansT1(i),
        measurement.netdDevsT0(i),
        measurement.netdDevsT1(i),
        measurement.temp0,
        measurement.temp1
      )).toArray

    measurement.netd0 = (for (i <- 0 until 384 * 288) yield netds(i).head).toArray
    measurement.netd1 = (for (i <- 0 until 384 * 288) yield netds(i)(1)).toArray

    netdDone.set(true)
  }

  def captureNetdImage(i: Int): Unit = {
    val numFrames = netdFrames.value.toInt
    val frames = captureImageSequence(numFrames)
    val m = (for (i <- 0 until 384 * 288) yield mean(for (j <- 0 until numFrames) yield frames(j)(i))).toArray
    val u = (for (i <- 0 until 384 * 288) yield stdev(for (j <- 0 until numFrames) yield frames(j)(i))).toArray
    val temp = netdTemp(i).value.toDouble
    if (i == 0) {
      measurement.netdMeansT0 = m
      measurement.netdDevsT0 = u
      measurement.temp0 = temp
      t0Set.set(true)
    } else {
      measurement.netdMeansT1 = m
      measurement.netdDevsT1 = u
      measurement.temp1 = temp
      t1Set.set(true)
    }
  }

  val t0Set = BooleanProperty(value = false)
  val t1Set = BooleanProperty(value = false)

  val netdTemp = ObservableBuffer(Seq(
    StringProperty(""),
    StringProperty("")
  ))

  val netdFrames = StringProperty("")

  var measurement: Measurement = new Measurement()

  val fileName = StringProperty("")

  val noiseFrames = StringProperty("")

  def save(): Unit = {
    measurement.save(fileName.value)
  }

  def load(): Unit = {
    measurement = Measurement.fromFile(fileName.value)
    // TODO: Initialize properties using measurement if necessary.
    Measurement.name.set(measurement.name)
    // TODO: This sort of usage of Measurement object is ugly. Find a better way.
  }

  def captureImageSequence(numFrames: Int): IndexedSeq[IndexedSeq[Int]] = {
    for (i <- 0 until numFrames) yield {
      val rawFrame = fp.getClippedFrameData
      val frame = for (i <- 0 until 384 * 288) yield {
        rawFrame(2 * i) + rawFrame(2 * i + 1) * 256
      }
      frame
    }
  }

  def captureAverageFrame: IndexedSeq[Int] = {
    val darkFrames = captureImageSequence(64)
    for (i <- 0 until 384 * 288) yield (for (j <- 0 until 64) yield darkFrames(j)(i)).sum / 64
  }

  def captureDarkImage(): Unit = {
    val meanDarkFrame = captureAverageFrame
    measurement.dark = meanDarkFrame.toArray
    measurement.darkSet = true
    measurement.calculateSlope()
  }

  def captureGrayImage(): Unit = {
    val meanGrayFrame = captureAverageFrame
    measurement.gray = meanGrayFrame.toArray
    measurement.graySet = true
    measurement.calculateSlope()
  }

  // TODO: Check if sum hits Int.MaxValue
  def mean(seq: Seq[Int]): Double = {
    seq.sum.toDouble / seq.length
  }

  def stdev(seq: Seq[Int]): Double = {
    val m = mean(seq)
    spire.math.sqrt(seq.map(a => (a.toDouble - m) * (a.toDouble - m)).sum / seq.length)
  }

  def measureNoise(): Unit = {
    val numFrames = noiseFrames.value.toInt
    val frames = captureImageSequence(numFrames)
    measurement.noise = (for (i <- 0 until 384 * 288) yield stdev(for (j <- 0 until numFrames) yield frames(j)(i))).toArray
  }

  val fNumber = StringProperty("1.2")
  val detectorDimension = StringProperty("25")
  val netdDone = BooleanProperty(value = false)

  def measureResponsivity(): Unit = {
    val f = fNumber.value.toDouble
    val dim = detectorDimension.value.toDouble * 0.000001
    val area = dim * dim

    measurement.responsivity = (for (i <- 0 until 384 * 288) yield
      (measurement.netdDevsT0(i) * (4 * f * f + 1)) / (area * 2.62 * measurement.netd0(i))
      ).toArray
  }

  def combineBytes(raw: Array[Byte]): Array[Int] = {
    (for (i <- 0 until raw.length / 2) yield {
      ((raw(2 * i)+256) % 256) + ((raw(2 * i + 1) + 256) % 256) * 256
    }).toArray
  }
  def dc = FpgaController.deviceController

  val vRef = StringProperty("Not set")
  val vDet = StringProperty("Not set")

  def createResistorMap(): Unit = {
    if (FpgaController.selectedModel.value == "A1") {
      createA1ResistorMaps()
    } else {
      createA0ResistorMap()
      createA0ReferenceResistorMap()
    }
  }

  def createA1ResistorMaps(): Unit = {
    // TODO: Pending redefinition
    dc.disableImagingMode()
    dc.setReset()
    dc.clearReset()
    dc.initializeRoic()
    dc.setNucMode(NucMode.Fixed,255)
    dc.sendReferenceDataToRoic()
    dc.setNucMode(NucMode.Fixed,254)
    dc.setTriggerMode(TriggerMode.Slave_Software)
    if (FpgaController.isCmosTest.value) {
      dc.writeToRoicMemory(17,3)
    }
    dc.setIntegrationTime(30)
    dc.setPixelGain(31)
    dc.setPixelBiasRange(1248)
    dc.setActiveFlashPartition(0)
    dc.writeToRoicMemory(7,2063)
    dc.updateRoicMemory()
    dc.writeToRoicMemory(18,12)
    dc.updateRoicMemory()
    dc.enableImagingMode()

    var pFrame = combineBytes(dc.getFullFrame)
    var frame = combineBytes(dc.getFullFrame)
    val refRes = Array.ofDim[Double](384 * 288)
    val refMean = Array.ofDim[Double](384 * 12)
    val deltas = Array.ofDim[Double](384 * 288)
    val pixValues0 = Array.ofDim[Int](384 * 288)
    val refBiasValues0 = Array.ofDim[Int](384 * 288)
    val pixValues1 = Array.ofDim[Int](384 * 288)
    val refBiasValues1 = Array.ofDim[Int](384 * 288)

    for (j <- 1 to 23) {
      val biasVal = 70 + j*70
      dc.setGlobalReferenceBias(biasVal)
      dc.updateRoicMemory()
      pFrame = frame
      frame = combineBytes(dc.getFrame)
      for (i <- frame.indices) {
        if(pFrame(i) <= 13000 && frame(i) >= 13000) {
          val k = 88086021.51
          val a = pFrame(i)
          val b = frame(i)
          refRes(i) = k / math.abs(frame(i) - pFrame(i))
          deltas(i) = math.abs(frame(i) - pFrame(i))
          refMean(((i/384)%12)*384+(i%384)) = refMean(((i/384)%12)*384+(i%384)) + refRes(i)
          pixValues0(i) = frame(i)
          refBiasValues0(i) = j
        }
      }

    }

    dc.disableImagingMode()
    dc.setNucMode(NucMode.Fixed,242)
    dc.enableImagingMode()

    for (j <- 1 to 19) {
      val biasVal = 70 + j*70
      dc.setGlobalReferenceBias(biasVal)
      dc.updateRoicMemory()
      pFrame = frame
      frame = combineBytes(dc.getFrame)
      for (i <- frame.indices) {
        if(pFrame(i) <= 13000 && frame(i) >= 13000) {
          pixValues1(i) = frame(i)
          refBiasValues1(i) = j
        }
      }

    }

    val res = for (i <- refRes.indices) yield {
      val K = 5.676e-10
      val a = (pixValues0(i) - pixValues1(i))*K
      val b = 50e-3*(refBiasValues0(i) - refBiasValues1(i))/(refRes(i) + K)
      deltas(i) = pixValues0(i) - pixValues1(i)
      refRes(i) = b
      val c = (a - b + K)
      66e-3/c   //51e-3
    }

    for (i <- refMean.indices) {
      refMean(i) = refMean(i)/24
    }
    measurement.resistorMap = res.toArray
    measurement.referenceResistorMap = refMean.toArray
  }

  def fp = FpgaController.frameProvider

  def createA0ResistorMap(): Unit = {

    def findVmeas(cur: Int, min: Int, max: Int, isDetector: Boolean): Int = {
      def isVmeas(vmid: Int): Int = {
        dc.setPixelMidpoint(vmid)
        val rawFrame = fp.getFrameData.drop(392 * 2)
        val frame = combineBytes(rawFrame)
        val vavgFrame = frame.zipWithIndex.filter(_._2 % 392 < 384).map(_._1)
        val vavg = vavgFrame.map(_.toDouble).sum / vavgFrame.length
        val vdcFrame = frame.zipWithIndex.filter(_._2 % 392 >= 384).map(_._1)
        val vdc = vdcFrame.map(_.toDouble).sum / vdcFrame.length
        if (vavg - vdc < 900) -1
        else if (vavg - vdc > 950) 1
        else 0
      }

      val isGood = isVmeas(cur)

      val searchDirection = if (isDetector) 1 else -1

      if (isGood == 0) cur
      else if (isGood == searchDirection) {
        if (min < cur) {
          val newMax = cur - 1
          val newCur = (cur + min) / 2
          findVmeas(newCur, min, newMax, isDetector)
        }
        else throw new Exception("No Vmeas found.")
      }
      else {
        if (max > cur) {
          val newMin = cur + 1
          val newCur = (cur + max + 1) / 2
          findVmeas(newCur, newMin, max, isDetector)
        }
        else throw new Exception("No Vmeas found.")
      }
    }

    dc.setReset()
    dc.clearReset()
    dc.disableImagingMode()
    dc.initializeRoic()
    dc.setNucMode(NucMode.Fixed,0)
    dc.sendReferenceDataToRoic()
    dc.setTriggerMode(TriggerMode.Slave_Software)
    dc.setResistanceMeasurementMode(ResistanceMeasurementMode.Detector)
    dc.setIntegrationTime(30)
    dc.writeToRoicMemory(22,2047)
    dc.readFromRoicMemory(22)
    dc.writeToRoicMemory(18,4)
    if (FpgaController.isCmosTest.value) {
      dc.writeToRoicMemory(17, 3)
    }
    dc.enableImagingMode()

    val vmeas = findVmeas(2000, 750, 3250, isDetector = true)

    vDet.set(vmeas.toString)

    dc.setPixelMidpoint(vmeas - 8)
    val f1 = fp.getClippedFrame
    dc.setPixelMidpoint(vmeas + 8)
    val f2 = fp.getClippedFrame

    val s1 = for (i <- f1.indices) yield f2(i) - f1(i)

    dc.setIntegrationTime(0)
    dc.setPixelMidpoint(vmeas - 8)
    val f3 = fp.getClippedFrame
    dc.setPixelMidpoint(vmeas + 8)
    val f4 = fp.getClippedFrame

    val s2 = for (i <- f3.indices) yield f4(i) - f3(i)

    val deltaV = for (i <- f1.indices) yield math.abs(f1(i) - f3(i))

    val tint: Double = 0.00001
    val cint: Double = 0.000000000031
    val k: Double = 270.0
    val r = for (i <- f1.indices) yield (tint / cint) * ((s2(i).toDouble / (s1(i).toDouble - s2(i).toDouble + 0.000000001)) - (k / (deltaV(i).toDouble + 0.000000001)))

    measurement.resistorMap = r.toArray
  }

  def createA0ReferenceResistorMap(): Unit = {

    def findVmeas(cur: Int, min: Int, max: Int, isDetector: Boolean): Int = {
      def isVmeas(vmid: Int): Int = {
        dc.setPixelMidpoint(vmid)
        val rawFrame = fp.getFrameData.drop(392 * 2)
        val frame = combineBytes(rawFrame)
        val vavgFrame = frame.zipWithIndex.filter(_._2 % 392 < 384).map(_._1)
        val vavg = vavgFrame.map(_.toDouble).sum / vavgFrame.length
        val vdcFrame = frame.zipWithIndex.filter(_._2 % 392 >= 384).map(_._1)
        val vdc = vdcFrame.map(_.toDouble).sum / vdcFrame.length
        if (vdc-vavg < 900) -1
        else if (vdc-vavg > 950) 1
        else 0
      }

      val isGood = isVmeas(cur)

      val searchDirection = if (isDetector) 1 else -1

      if (isGood == 0) cur
      else if (isGood == searchDirection) {
        if (min < cur) {
          val newMax = cur - 1
          val newCur = (cur + min) / 2
          findVmeas(newCur, min, newMax, isDetector)
        }
        else throw new Exception("No Vmeas found.")
      }
      else {
        if (max > cur) {
          val newMin = cur + 1
          val newCur = (cur + max + 1) / 2
          findVmeas(newCur, newMin, max, isDetector)
        }
        else throw new Exception("No Vmeas found.")
      }
    }

    dc.setReset()
    dc.clearReset()
    dc.disableImagingMode()
    dc.initializeRoic()
    dc.setNucMode(NucMode.Fixed,255)
    dc.sendReferenceDataToRoic()
    dc.setTriggerMode(TriggerMode.Slave_Software)
    dc.setResistanceMeasurementMode(ResistanceMeasurementMode.Reference)
    dc.setNucMode(NucMode.Disabled)
    dc.setIntegrationTime(30)
    dc.writeToRoicMemory(22,2047)
    dc.writeToRoicMemory(18,12)
    if (FpgaController.isCmosTest.value) {
      dc.writeToRoicMemory(17, 3)
    }
    dc.enableImagingMode()

    val vmeas = findVmeas(2000, 750, 3250, isDetector = false)

    vRef.set(vmeas.toString)

    dc.setPixelMidpoint(vmeas - 8)
    val f1 = fp.getClippedFrame.slice(384 * 11, 384 * 11 + 384 * 12)
    dc.setPixelMidpoint(vmeas + 8)
    val f2 = fp.getClippedFrame.slice(384 * 11, 384 * 11 + 384 * 12)

    val s1 = for (i <- f1.indices) yield f2(i) - f1(i)

    dc.setIntegrationTime(0)
    dc.setPixelMidpoint(vmeas - 8)
    val f3 = fp.getClippedFrame.slice(384 * 11, 384 * 11 + 384 * 12)
    dc.setPixelMidpoint(vmeas + 8)
    val f4 = fp.getClippedFrame.slice(384 * 11, 384 * 11 + 384 * 12)

    val s2 = for (i <- f3.indices) yield f4(i) - f3(i)

    val deltaV = for (i <- f1.indices) yield math.abs(f1(i) - f3(i))

    import spire.implicits._
    val tint: Double = (10.0 pow -6) * 10
    val cint: Double = (10.0 pow -12) * 31
    val k: Double = 166.0
    val r = for (i <- f1.indices) yield (tint / cint) * ((s2(i).toDouble / (s1(i).toDouble - s2(i).toDouble + 0.000000001)) - (k / (deltaV(i).toDouble + 0.000000001)))

    measurement.referenceResistorMap = r.toArray
  }

  val measurementLabels = ObservableBuffer(Seq(
    "NETD T0",
    "NETD T1",
    "Resistor Map - Detectors",
    "Resistor Map - References",
    "Responsivity",
    "Noise",
    "Pixel Values"
  ))

  val selectedMeasurement = StringProperty("NETD T0")

  selectedMeasurement.onChange((_,_,_) => {
    if (selectedMeasurement.value == "NETD T0") {
      val data = measurement.netd0
      measurementDisplayMin set data.min.toString
      measurementDisplayMax set data.max.toString
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "NETD T1") {
      val data = measurement.netd1
      measurementDisplayMin set data.min.toString
      measurementDisplayMax set data.max.toString
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "Resistor Map - Detectors") {
      val data = measurement.resistorMap
      measurementDisplayMin set data.min.toString
      measurementDisplayMax set data.max.toString
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "Resistor Map - References") {
      val data = measurement.referenceResistorMap
      measurementDisplayMin set data.min.toString
      measurementDisplayMax set data.max.toString
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "Responsivity") {
      val data = measurement.responsivity
      measurementDisplayMin set data.min.toString
      measurementDisplayMax set data.max.toString
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "Noise") {
      val data = measurement.noise
      measurementDisplayMin set data.min.toString
      measurementDisplayMax set data.max.toString
      updateMeasurementDisplayWithContinuousData(data.toSeq)
    } else { // Pixel values
      val frame = ImageController.currentFrame
      measurementDisplayMin set frame.data.min.toString
      measurementDisplayMax set frame.data.max.toString
      heatmap.set(SwingFXUtils.toFXImage(frame.getHeatmap, null))
      updateMeasurementDisplayWithPixelValues()
    }
  })

  def handleDisplayRangeChange(): Unit = {
    if (selectedMeasurement.value == "NETD T0") {
      val data = measurement.netd0
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "NETD T1") {
      val data = measurement.netd1
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "Resistor Map - Detectors") {
      val data = measurement.resistorMap
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "Resistor Map - References") {
      val data = measurement.referenceResistorMap
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "Responsivity") {
      val data = measurement.responsivity
      updateMeasurementDisplayWithContinuousData(data)
    } else if (selectedMeasurement.value == "Noise") {
      val data = measurement.noise
      updateMeasurementDisplayWithContinuousData(data.toSeq)
    } else {
      updateMeasurementDisplayWithPixelValues()
    }
  }

  def updateMeasurementDisplayWithPixelValues(): Unit = {
    val frame = ImageController.currentFrame
    val min = measurementDisplayMin.value.toInt
    val max = measurementDisplayMax.value.toInt
    val cutFrame = frame.minMaxCut(min, max)

    heatmap.set(SwingFXUtils.toFXImage(cutFrame.getHeatmap, null))

    val croppedData = frame.data.filter(n => n > measurementDisplayMin.value.toInt && n < measurementDisplayMax.value.toInt)
    val histogramFrame = new Frame(croppedData.length, 1, croppedData, frame.depth)
    histogram.clear()
    histogram += histogramFrame.histogramData(measurementDisplayMin.value.toInt, measurementDisplayMax.value.toInt, histogramBinCount.value.toInt)
//    displayPeak.set(histogramFrame.histogram().indexOf(histogramFrame.histogram().max).toString)
//    displayMean.set((histogramFrame.data.map(_.toDouble).sum / histogramFrame.data.length).toString)
  }

  def histogramData(min: Double, max: Double, steps: Int, data: Seq[Double]): XYChart.Series[String, Number] = {
    val stepSize = (max - min) / steps
    val hist = Array.ofDim[Int](steps)
    for (d <- data) {
      if (d >= min && d < max) {
        hist(((d-min)/stepSize).toInt) += 1
      }
    }
    val labels = min until max by stepSize
    val series = new XYChart.Series[String, Number] {
      name = "Histogram"
      data = labels zip hist map {
        case (x,y) => XYChart.Data[String, Number](x.toString, y)
      }
    }
    series
  }

  def updateMeasurementDisplayWithContinuousData(data: Seq[Double]): Unit = {
    val min = measurementDisplayMin.value.toDouble
    val max = measurementDisplayMax.value.toDouble
    val bins = histogramBinCount.value.toInt
    val noiseFrame = Frame.createFromContinuousData(
      FpgaController.xSize.value.toInt,
      FpgaController.ySize.value.toInt,
      min,
      max,
      data,
      16383
    )
    heatmap.set(SwingFXUtils.toFXImage(noiseFrame.getHeatmap, null))

    histogram.clear()
    histogram += histogramData(min, max, bins, data)
  }

  val diagonalData = Array.ofDim[Int](384*288)
  for (i <- 0 until 384) for (j <- 0 until 288) diagonalData(j * 384 + i) = 8192 * i / 383 + 8192 * j / 287
  val diagonalFrame = Frame.createFrom14Bit(384, 288, diagonalData)

  val randomData = Array.ofDim[Int](384*288)
  for (i <- randomData.indices) randomData(i) = Random.nextInt(16383)
  val randomFrame = Frame.createFrom14Bit(384, 288, randomData)

  val croppedData = Array.ofDim[Int](384*24)
  for (i <- croppedData.indices) croppedData(i) = Random.nextInt(16383)
  val croppedFrame = Frame.createFrom14Bit(384, 24, croppedData)

  val heatmap = ObjectProperty[Image](SwingFXUtils.toFXImage(diagonalFrame.getHeatmap, null))
  val histogram = ObservableBuffer[javafx.scene.chart.XYChart.Series[String, Number]](diagonalFrame.histogramData(0, 16383, 128))

  val displayMean = StringProperty("n/a")
  val displayPeak = StringProperty("n/a")

  val measurementDisplayMax = StringProperty("")
  val measurementDisplayMin = StringProperty("")

  val histogramBinCount = StringProperty("128")
}
