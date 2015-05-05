package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.command.ApiConstants.ResistanceMeasurementMode
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

    measurement.netd0 = netds(0).toArray
    measurement.netd1 = netds(1).toArray
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
    spire.math.sqrt(seq.map(a => (a.toDouble - m) * (a.toDouble - m)).sum)
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

  def createResistorMap(): Unit = {

    val dc = FpgaController.deviceController

    dc.setReset()
    dc.clearReset()
    dc.updateReferenceData(Array.ofDim[Byte](384 * 2))
    dc.initializeRoic()
    dc.setResistanceMeasurementMode(ResistanceMeasurementMode.Detector)
    dc.setIntegrationTime(30)
    dc.enableImagingMode()

    def find(cur: Int, min: Int, max: Int): Int = {
      def isVmeas(vmid: Int): Int = {
        dc.setPixelMidpoint(vmid)
        val rawFrame = dc.getFullFrame.drop(392)
        val frame = for (i <- 0 until 392 * 289) yield {
          rawFrame(2 * i) + rawFrame(2 * i + 1) * 256
        }
        val vavgFrame = frame.zipWithIndex.filter(_._2 % 392 >= 384).map(_._1)
        val vavg = vavgFrame.map(_.toDouble).sum / vavgFrame.length
        val vdcFrame = frame.zipWithIndex.filter(_._2 % 392 < 384).map(_._1)
        val vdc = vdcFrame.map(_.toDouble).sum / vavgFrame.length
        if (vavg - vdc < 900) -1
        else if (vavg - vdc > 950) 1
        else 0
      }

      val isGood = isVmeas(cur)

      if (isGood == 0) cur
      else if (isGood == -1) {
        if (min < cur) {
          val newMax = cur - 1
          val newCur = (cur + min) / 2
          find(newCur, min, newMax)
        }
        else throw new Exception("No Vmeas found.")
      }
      else {
        if (max > cur) {
          val newMin = cur + 1
          val newCur = (cur + max + 1) / 2
          find(newCur, newMin, max)
        }
        else throw new Exception("No Vmeas found.")
      }
    }

    val vmeas = find(2000, 1000, 3000)

    dc.setPixelMidpoint(vmeas - 8)
    val f1 = dc.getFrame
    dc.setPixelMidpoint(vmeas + 8)
    val f2 = dc.getFrame

    val s1 = for (i <- 0 until f1.length) yield f2(i) - f1(i)

    dc.setIntegrationTime(0)
    dc.setPixelMidpoint(vmeas - 8)
    val f3 = dc.getFrame
    dc.setPixelMidpoint(vmeas + 8)
    val f4 = dc.getFrame

    val s2 = for (i <- 0 until f3.length) yield f4(i) - f3(i)

    val deltaV = for (i <- 0 until f1.length) yield f1(i) - f3(i)

    import spire.implicits._
    val tint: Double = 10 pow -6
    val cint: Double = (10 pow -12) * 31
    val k: Double = 270.0
    val r = for (i <- 0 until f1.length) yield (tint / cint) * ((s2(i) / (s1(i) - s2(i))) - (k / deltaV(i)))

    measurement.resistorMap = r.toArray
  }
}
