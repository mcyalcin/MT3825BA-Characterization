package com.mikrotasarim.ui.controller

import com.mikrotasarim.image.Frame

import scalafx.beans.property.StringProperty

object ImageController {

  val filePrefix = new StringProperty("")
  val sampleCount = new StringProperty("")

  def saveImages(): Unit = {
    for (i <- 0 until sampleCount.value.toInt) {
      val frame = Frame.fromProcessed(getImage)
      frame.saveTiff(filePrefix.value + "_" + i + ".tif")
      println("Sample " + filePrefix.value + "_" + i + ".tif saved.")
    }
  }

  def openImage(): Unit = {
    val frame = Frame.fromProcessed(getImage)
    frame.saveTiff(filePrefix.value + "_temp.tif")
    Frame.show(filePrefix.value + "_temp.tif")
  }

  def getImage: Array[Int] = {
    def combineBytes(raw: Array[Byte]): Array[Int] =
      (for (i <- 0 until 384 * 288) yield raw(2 * i) + raw(2 * i + 1) * 256).toArray

    def onePointCorrect(img: Array[Int]): Array[Int] =
      (for (i <- 0 until 384 * 288) yield Seq(0, img(i) - MeasurementController.measurement.dark(i)).max).toArray

    def twoPointCorrect(img: Array[Int]): Array[Int] =
      img // TODO: Implement

    val rawFrame = FpgaController.deviceController.getFrame
    val frame = combineBytes(rawFrame)

    val correctedFrame =
      if (FpgaController.correctionEnabled.value)
        if (FpgaController.onePointCorrection.value) onePointCorrect(frame)
        else twoPointCorrect(frame)
      else frame

    correctedFrame
  }
}
