package com.mikrotasarim.ui.model

import com.mikrotasarim.image.Frame
import com.mikrotasarim.ui.controller.FpgaController

import scalafx.beans.property.StringProperty

object FileControlModel {

  val filePrefix = new StringProperty()
  val sampleCount = new StringProperty()

  def saveImages(): Unit = {
    for (i <- 0 until sampleCount.value.toInt) {
      // TODO: Implement acquisition and persistence of images
      println("Sample " + filePrefix.value + "_" + i + ".tif saved.")
    }
  }

  def openImage(): Unit = {
    val rawFrame = FpgaController.DeviceController.getFrame
    println(rawFrame.length)
    Frame.show()
  }
}
