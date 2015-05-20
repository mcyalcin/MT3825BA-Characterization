package com.mikrotasarim.ui.controller

import javafx.embed.swing.SwingFXUtils

import com.mikrotasarim.ui.model.Frame

import scalafx.beans.property.{ObjectProperty, StringProperty}
import javafx.scene.image.Image

object ImageController {

  val filePrefix = new StringProperty("d:\\")
  val sampleCount = new StringProperty("")

  def xSize = FpgaController.xSize.value.toInt
  def ySize = FpgaController.ySize.value.toInt

  def saveImages(): Unit = {
    for (i <- 0 until sampleCount.value.toInt) {
      val frame = Frame.createFrom14Bit(xSize, ySize, getImage)
      frame.save(filePrefix.value + "_" + i + ".tif")
    }
  }

  def refreshImage(): Unit = {
    val frame = Frame.createFrom14Bit(xSize, ySize, getImage)
    currentImage.set(SwingFXUtils.toFXImage(frame.getGrayscale, null))
  }

  def getImage: Array[Int] = {

    def onePointCorrect(img: Array[Int]): Array[Int] =
      (for (i <- 0 until 384 * 288) yield Seq(0, img(i) - MeasurementController.measurement.dark(i)).max).toArray

    def twoPointCorrect(img: Array[Int]): Array[Int] = {
      (for (i <- 0 until 384 * 288) yield
        (MeasurementController.measurement.slope(i) *
          Seq(0, img(i) - MeasurementController.measurement.dark(i)).max).toInt).toArray
    }

    val frame = getRawImage

    val correctedFrame =
      if (FpgaController.correctionEnabled.value)
        if (FpgaController.onePointCorrection.value) onePointCorrect(frame)
        else twoPointCorrect(frame)
      else frame

    correctedFrame
  }

  def getRawImage: Array[Int] = {

    def combineBytes(raw: Array[Byte]): Array[Int] = {
      def unsigned(b: Byte): Int = {
        (b.toInt + 256) % 256
      }
      (for (i <- 0 until 384 * 288) yield unsigned(raw(2 * i)) + unsigned(raw(2 * i + 1)) * 256).toArray
    }

    val rawFrame = FpgaController.deviceController.getFrame
    combineBytes(rawFrame)
  }

  def dc = FpgaController.deviceController

  val diagonalData = Array.ofDim[Int](384*288)
  for (i <- 0 until 384) for (j <- 0 until 288) diagonalData(j * 384 + i) = 8192 * i / 383 + 8192 * j / 287
  val diagonalFrame = Frame.createFrom14Bit(384, 288, diagonalData)

  val currentImage = ObjectProperty[Image](SwingFXUtils.toFXImage(diagonalFrame.getGrayscale, null))
}
