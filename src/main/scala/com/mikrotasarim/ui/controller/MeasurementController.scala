package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.command.ApiConstants.{NucMode, TriggerMode, ResistanceMeasurementMode}
import com.mikrotasarim.ui.model.Measurement

import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.ObservableBuffer

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

  def measureResponsivity(): Unit = {
    measurement.responsivity = (for (i <- 0 until 384 * 288) yield
      (measurement.netdMeansT0(i) - measurement.netdMeansT1(i)) / (measurement.temp0 - measurement.temp1)
      ).toArray
  }

  def combineBytes(raw: Array[Byte]): Array[Int] = {
    (for (i <- 0 until raw.length / 2) yield {
      ((raw(2 * i)+256) % 256) + ((raw(2 * i + 1) + 256) % 256) * 256
    }).toArray
  }
  def dc = FpgaController.deviceController

  def createResistorMap(): Unit = {

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

    val vmeas = findVmeas(2000, 1000, 3000, isDetector = true)

    dc.setPixelMidpoint(vmeas - 8)
    val f1 = combineBytes(dc.getFrame)
    dc.setPixelMidpoint(vmeas + 8)
    val f2 = combineBytes(dc.getFrame)

    val s1 = for (i <- 0 until f1.length) yield f2(i) - f1(i)

    dc.setIntegrationTime(0)
    dc.setPixelMidpoint(vmeas - 8)
    val f3 = combineBytes(dc.getFrame)
    dc.setPixelMidpoint(vmeas + 8)
    val f4 = combineBytes(dc.getFrame)

    val s2 = for (i <- 0 until f3.length) yield f4(i) - f3(i)

    val deltaV = for (i <- 0 until f1.length) yield math.abs(f1(i) - f3(i))

    val tint: Double = 0.00001
    val cint: Double = 0.000000000031
    val k: Double = 270.0
    val r = for (i <- 0 until f1.length) yield (tint / cint) * ((s2(i).toDouble / (s1(i).toDouble - s2(i).toDouble + 0.000000001)) - (k / (deltaV(i).toDouble + 0.000000001)))

    measurement.resistorMap = r.toArray
  }

  def createReferenceResistorMap(): Unit = {

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

    val vmeas = findVmeas(2000, 1000, 3000, isDetector = false)

    dc.setPixelMidpoint(vmeas - 8)
    val f1 = combineBytes(dc.getFrame).drop(384*11).take(384*12)
    dc.setPixelMidpoint(vmeas + 8)
    val f2 = combineBytes(dc.getFrame).drop(384*11).take(384*12)

    val s1 = for (i <- 0 until f1.length) yield f2(i) - f1(i)

    dc.setIntegrationTime(0)
    dc.setPixelMidpoint(vmeas - 8)
    val f3 = combineBytes(dc.getFrame).drop(384*11).take(384*12)
    dc.setPixelMidpoint(vmeas + 8)
    val f4 = combineBytes(dc.getFrame).drop(384*11).take(384*12)

    val s2 = for (i <- 0 until f3.length) yield f4(i) - f3(i)

    val deltaV = for (i <- 0 until f1.length) yield math.abs(f1(i) - f3(i))

    import spire.implicits._
    val tint: Double = (10.0 pow -6) * 10
    val cint: Double = (10.0 pow -12) * 31
    val k: Double = 166.0 // Old Value : 196
    val r = for (i <- 0 until f1.length) yield (tint / cint) * ((s2(i).toDouble / (s1(i).toDouble - s2(i).toDouble + 0.000000001)) - (k / (deltaV(i).toDouble + 0.000000001)))

    measurement.referenceResistorMap = r.toArray
  }
}
