package com.mikrotasarim.ui.controller

import com.mikrotasarim.ui.model.Measurement

import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.ObservableBuffer

object MeasurementController {
  def measureNetd(): Unit = {
    def netd(m0: Double, m1: Double, u0: Double, u1: Double, t0: Double, t1: Double): Seq[Double] =
      Seq(u0 / math.abs(m0 - m1) * math.abs(t0-t1), u1 / math.abs(m0 - m1) * math.abs(t0-t1))

    val netds = (for (i <- 0 until 384*288) yield
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
    val m = (for (i <- 0 until 384*288) yield mean(for (j <- 0 until numFrames) yield frames(j)(i))).toArray
    val u = (for (i <- 0 until 384*288) yield stdev(for (j <- 0 until numFrames) yield frames(j)(i))).toArray
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
    for (i <- 0 until 384*288) yield (for (j <- 0 until 64) yield darkFrames(j)(i)).sum / 64
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
    measurement.noise = (for (i <- 0 until 384*288) yield stdev(for (j <- 0 until numFrames) yield frames(j)(i))).toArray
  }
}
