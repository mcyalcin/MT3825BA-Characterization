package com.mikrotasarim.ui.controller

import javafx.embed.swing.SwingFXUtils

import com.mikrotasarim.ui.model.{FrameProvider, Frame}

import scalafx.beans.property.{BooleanProperty, ObjectProperty, StringProperty}
import javafx.scene.image.Image

object ImageController {

  val filePrefix = new StringProperty("d:\\")
  val sampleCount = new StringProperty("")
  val histEqSelected = new BooleanProperty() {
    value = false
  }

  def xSize = FpgaController.xSize.value.toInt

  def ySize = FpgaController.ySize.value.toInt

  def saveImages(): Unit = {
    for (i <- 0 until sampleCount.value.toInt) {
      val frame = FpgaController.frameProvider.getFrame
      frame.save(filePrefix.value + "_" + i + ".tif")
    }
  }

  val diagonalData = Array.ofDim[Int](384 * 288)
  for (i <- 0 until 384) for (j <- 0 until 288) diagonalData(j * 384 + i) = 8192 * i / 383 + 8192 * j / 287
  val diagonalFrame = Frame.createFrom14Bit(384, 288, diagonalData)

  var currentFrame: Frame = Frame.createFrom14Bit(384, 288, diagonalData)

  def fp: FrameProvider = FpgaController.frameProvider

  def refreshImage(): Unit = {
    val newFrame = fp.getFrame
    currentFrame = if (histEqSelected.value) {
      newFrame.topBotCut()
    } else {
      newFrame
    }
    currentImage.set(SwingFXUtils.toFXImage(currentFrame.getGrayscale, null))
  }

  def correctImage(frame: Array[Int]): Array[Int] = {
    def onePointCorrect(img: Array[Int]): Array[Int] =
      (for (i <- 0 until 384 * 288) yield Seq(0, img(i) - MeasurementController.measurement.dark(i)).max).toArray

    def twoPointCorrect(img: Array[Int]): Array[Int] = {
      (for (i <- 0 until 384 * 288) yield
      (MeasurementController.measurement.slope(i) *
        Seq(0, img(i) - MeasurementController.measurement.dark(i)).max).toInt).toArray
    }

    if (FpgaController.correctionEnabled.value)
      if (FpgaController.onePointCorrection.value) onePointCorrect(frame)
      else twoPointCorrect(frame)
    else frame
  }

  def combineBytes(raw: Array[Byte]): Array[Int] = {
    def unsigned(b: Byte): Int = {
      (b.toInt + 256) % 256
    }
    (for (i <- 0 until 384 * 288) yield unsigned(raw(2 * i)) + unsigned(raw(2 * i + 1)) * 256).toArray
  }

  def dc = FpgaController.deviceController

  val currentImage = ObjectProperty[Image](SwingFXUtils.toFXImage(diagonalFrame.getGrayscale, null))
}
