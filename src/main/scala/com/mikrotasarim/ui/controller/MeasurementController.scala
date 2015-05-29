package com.mikrotasarim.ui.controller

import javafx.embed.swing.SwingFXUtils

import com.mikrotasarim.api.command.ApiConstants.{NucMode, TriggerMode, ResistanceMeasurementMode}
import com.mikrotasarim.ui.model.{Frame, Measurement}

import scala.util.Random
import scalafx.beans.property.{ObjectProperty, BooleanProperty, StringProperty}
import scalafx.collections.ObservableBuffer

import javafx.scene.image.Image

import javafx.scene.chart.XYChart

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
      val rawFrame = FpgaController.deviceController.getFrame
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
    if (FpgaController.selectedBitfile.value == "A1") {
      createA1ResistorMaps()
    } else {
      createA0ResistorMap()
      createA0ReferenceResistorMap()
    }
  }

  def createA1ResistorMaps(): Unit = {
    dc.setReset()
    dc.clearReset()
    dc.initializeRoic()
    val ones = Array.fill[Byte](384*2)(255.toByte)
    dc.updateReferenceData(ones)
    dc.setIntegrationTime(30)
    dc.setPixelGain(31)
    dc.setGlobalReferenceBias(1365)
    dc.setPixelBiasRange(1248)
    dc.setActiveFlashPartition(0)
    CalibrationController.calculateAndApplyNuc()
    val frame0 = combineBytes(dc.getFullFrame)
    val nuc = CalibrationController.currentNuc
    // TODO: check this for sign issues. shouldn't be a problem up to 63 where we work.
    val shiftedNuc = nuc.map(a => a.map(n=> (if (n > 31) n-24 else n+24).toByte))
    dc.setActiveFlashPartition(1)
    dc.disableImagingMode()
    dc.eraseActiveFlashPartition()
    dc.writeFrameToFlashMemory(shiftedNuc)
    dc.setNucMode(NucMode.Enabled)
    dc.enableImagingMode()
    val frame1 = combineBytes(dc.getFullFrame)
    val k = 0.000000017617
    val r = for (i <- frame0.indices) yield k / (frame0(i) - frame1(i))
    measurement.resistorMap = r.toArray
    dc.setActiveFlashPartition(0)
    dc.disableImagingMode()
    dc.eraseActiveFlashPartition()
    dc.writeFrameToFlashMemory(shiftedNuc)
    dc.setNucMode(NucMode.Enabled)
    dc.enableImagingMode()
    val frameR0 = combineBytes(dc.getFullFrame)
    dc.setGlobalReferenceBias(1501)
    val frameR1 = combineBytes(dc.getFullFrame)
    val rr = for (i <- frameR0.indices) yield k / (frameR0(i) - frameR1(i))
    measurement.referenceResistorMap = rr.toArray
  }

  def createA0ResistorMap(): Unit = {

    def findVmeas(cur: Int, min: Int, max: Int, isDetector: Boolean): Int = {
      def isVmeas(vmid: Int): Int = {
        dc.setPixelMidpoint(vmid)
        val rawFrame = dc.getFullFrame.drop(392 * 2)
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
    val f1 = combineBytes(dc.getFrame)
    dc.setPixelMidpoint(vmeas + 8)
    val f2 = combineBytes(dc.getFrame)

    val s1 = for (i <- f1.indices) yield f2(i) - f1(i)

    dc.setIntegrationTime(0)
    dc.setPixelMidpoint(vmeas - 8)
    val f3 = combineBytes(dc.getFrame)
    dc.setPixelMidpoint(vmeas + 8)
    val f4 = combineBytes(dc.getFrame)

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
        val rawFrame = dc.getFullFrame.drop(392 * 2)
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
    val f1 = combineBytes(dc.getFrame).slice(384 * 11, 384 * 11 + 384 * 12)
    dc.setPixelMidpoint(vmeas + 8)
    val f2 = combineBytes(dc.getFrame).slice(384 * 11, 384 * 11 + 384 * 12)

    val s1 = for (i <- f1.indices) yield f2(i) - f1(i)

    dc.setIntegrationTime(0)
    dc.setPixelMidpoint(vmeas - 8)
    val f3 = combineBytes(dc.getFrame).slice(384 * 11, 384 * 11 + 384 * 12)
    dc.setPixelMidpoint(vmeas + 8)
    val f4 = combineBytes(dc.getFrame).slice(384 * 11, 384 * 11 + 384 * 12)

    val s2 = for (i <- f3.indices) yield f4(i) - f3(i)

    val deltaV = for (i <- f1.indices) yield math.abs(f1(i) - f3(i))

    import spire.implicits._
    val tint: Double = (10.0 pow -6) * 10
    val cint: Double = (10.0 pow -12) * 31
    val k: Double = 166.0 // Old Value : 196
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
    // TODO: Work out how to convert these values to frames
    if (selectedMeasurement.value == "NETD T0") {
      heatmap.set(SwingFXUtils.toFXImage(diagonalFrame.getHeatmap, null))
      histogram.clear()
      histogram += diagonalFrame.histogramData(0,16383, 128)
    } else if (selectedMeasurement.value == "NETD T1") {
      heatmap.set(SwingFXUtils.toFXImage(diagonalFrame.getHeatmap, null))
      histogram.clear()
      histogram += diagonalFrame.histogramData(0,16383, 128)
    } else if (selectedMeasurement.value == "Resistor Map - Detectors") {
      heatmap.set(SwingFXUtils.toFXImage(croppedFrame.getHeatmap, null))
      histogram.clear()
      histogram += croppedFrame.histogramData(0,16383, 128)
    } else if (selectedMeasurement.value == "Resistor Map - References") {
      heatmap.set(SwingFXUtils.toFXImage(randomFrame.getHeatmap, null))
      histogram.clear()
      histogram += randomFrame.histogramData(0,16383, 128)
    } else if (selectedMeasurement.value == "Responsivity") {
      heatmap.set(SwingFXUtils.toFXImage(randomFrame.getHeatmap, null))
      histogram.clear()
      histogram += randomFrame.histogramData(0,16383, 128)
    } else if (selectedMeasurement.value == "Noise") {
      val noiseArray = measurement.noise
      measurementDisplayMin set noiseArray.min.toString
      measurementDisplayMax set noiseArray.max.toString
      updateMeasurementDisplayWithNoise(noiseArray.toSeq)
    } else { // Pixel values
      val frame = ImageController.currentFrame
      heatmap.set(SwingFXUtils.toFXImage(frame.getHeatmap, null))
      updateMeasurementDisplayWithPixelValues()
    }
  })

  def updateMeasurementDisplayWithPixelValues(): Unit = {
    val frame = ImageController.currentFrame
    val croppedData = frame.data.filter(n => n > histogramMin.value.toInt && n < histogramMax.value.toInt)
    val histogramFrame = new Frame(croppedData.length, 1, croppedData, frame.depth)
    histogram.clear()
    histogram += histogramFrame.histogramData(histogramMin.value.toInt, histogramMax.value.toInt,128)
    displayPeak.set(histogramFrame.histogram().indexOf(histogramFrame.histogram().max).toString)
    displayMean.set((histogramFrame.data.map(_.toDouble).sum / histogramFrame.data.length).toString)
  }

  def updateMeasurementDisplayWithNoise(noiseArray: Seq[Double]): Unit = {
    val noiseFrame = Frame.createFromContinuousData(
      FpgaController.xSize.value.toInt,
      FpgaController.ySize.value.toInt,
      measurementDisplayMin.value.toDouble,
      measurementDisplayMax.value.toDouble,
      noiseArray,
      16383
    )
    heatmap.set(SwingFXUtils.toFXImage(noiseFrame.getHeatmap, null))
    histogram.clear()
    histogram += noiseFrame.histogramData(0,16383, 128)
    displayPeak.set(noiseFrame.histogram().indexOf(noiseFrame.histogram().max).toString)
    displayMean.set((noiseArray.sum / (FpgaController.xSize.value.toInt * FpgaController.ySize.value.toInt)).toString)
  }

//  measurementDisplayMin.onChange((_,_,_) => {
//    // TODO: Work out how to convert these values to frames
//    if (selectedMeasurement.value == "NETD T0") {
//      heatmap.set(SwingFXUtils.toFXImage(diagonalFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += diagonalFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "NETD T1") {
//      heatmap.set(SwingFXUtils.toFXImage(diagonalFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += diagonalFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "Resistor Map - Detectors") {
//      heatmap.set(SwingFXUtils.toFXImage(croppedFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += croppedFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "Resistor Map - References") {
//      heatmap.set(SwingFXUtils.toFXImage(randomFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += randomFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "Responsivity") {
//      heatmap.set(SwingFXUtils.toFXImage(randomFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += randomFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "Noise") {
//      val noiseArray = measurement.noise
//      measurementDisplayMin set noiseArray.min.toString
//      measurementDisplayMax set noiseArray.max.toString
//      updateMeasurementDisplayWithNoise(noiseArray.toSeq)
//    } else { // Pixel values
//    val image = ImageController.getImage
//      val frame = new Frame(
//        FpgaController.xSize.value.toInt,
//        FpgaController.ySize.value.toInt,
//        image,
//        16383
//      )
//      heatmap.set(SwingFXUtils.toFXImage(frame.getHeatmap, null))
//      histogram.clear()
//      histogram += frame.histogramData(0,16383,128)
//      displayPeak.set(frame.histogram().indexOf(frame.histogram().max).toString)
//      displayMean.set((image.map(_.toDouble).sum / (FpgaController.xSize.value.toInt * FpgaController.ySize.value.toInt)).toString)
//    }
//  })
//  measurementDisplayMax.onChange((_,_,_) => {
//    // TODO: Work out how to convert these values to frames
//    if (selectedMeasurement.value == "NETD T0") {
//      heatmap.set(SwingFXUtils.toFXImage(diagonalFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += diagonalFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "NETD T1") {
//      heatmap.set(SwingFXUtils.toFXImage(diagonalFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += diagonalFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "Resistor Map - Detectors") {
//      heatmap.set(SwingFXUtils.toFXImage(croppedFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += croppedFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "Resistor Map - References") {
//      heatmap.set(SwingFXUtils.toFXImage(randomFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += randomFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "Responsivity") {
//      heatmap.set(SwingFXUtils.toFXImage(randomFrame.getHeatmap, null))
//      histogram.clear()
//      histogram += randomFrame.histogramData(0,16383, 128)
//    } else if (selectedMeasurement.value == "Noise") {
//      val noiseArray = measurement.noise
//      measurementDisplayMin set noiseArray.min.toString
//      measurementDisplayMax set noiseArray.max.toString
//      updateMeasurementDisplayWithNoise(noiseArray.toSeq)
//    } else { // Pixel values
//    val image = ImageController.getImage
//      val frame = new Frame(
//        FpgaController.xSize.value.toInt,
//        FpgaController.ySize.value.toInt,
//        image,
//        16383
//      )
//      heatmap.set(SwingFXUtils.toFXImage(frame.getHeatmap, null))
//      histogram.clear()
//      histogram += frame.histogramData(0,16383,128)
//      displayPeak.set(frame.histogram().indexOf(frame.histogram().max).toString)
//      displayMean.set((image.map(_.toDouble).sum / (FpgaController.xSize.value.toInt * FpgaController.ySize.value.toInt)).toString)
//    }
//  })

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
  val histogram = ObservableBuffer[XYChart.Series[String, Number]](diagonalFrame.histogramData(0, 16383, 128))

  val displayMean = StringProperty("n/a")
  val displayPeak = StringProperty("n/a")

  val measurementDisplayMax = StringProperty("")
  val measurementDisplayMin = StringProperty("")

  val histogramMin = StringProperty("")
  val histogramMax = StringProperty("")
  val histogramBinCount = StringProperty("")
}
