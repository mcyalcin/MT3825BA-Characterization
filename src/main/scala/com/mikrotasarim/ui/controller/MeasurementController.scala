package com.mikrotasarim.ui.controller

import com.mikrotasarim.ui.model.Measurement

import scalafx.beans.property.StringProperty

object MeasurementController {

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

  def measureNoise(): Unit = {
    def stdev(seq: Seq[Int]): Double = {
      def mean(seq: Seq[Int]): Double = {
        seq.sum.toDouble / seq.length
      }

      val m = mean(seq)
      spire.math.sqrt(seq.map(a => (a.toDouble - m) * (a.toDouble - m)).sum)
    }

    val numFrames = noiseFrames.value.toInt
    val frames = captureImageSequence(numFrames)
    measurement.noise = (for (i <- 0 until 384*288) yield stdev(for (j <- 0 until 64) yield frames(j)(i))).toArray
  }
}
