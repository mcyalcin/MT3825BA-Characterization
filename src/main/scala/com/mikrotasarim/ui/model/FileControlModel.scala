package com.mikrotasarim.ui.model

import scalafx.beans.property.StringProperty

object FileControlModel {

  val filePrefix = new StringProperty()
  val sampleCount = new StringProperty()

  def SaveImages(): Unit = {
    for (i <- 0 until sampleCount.value.toInt) {
      // TODO: Implement acquisition and persistence of images
      println("Sample " + filePrefix.value + "_" + i + ".tif saved.")
    }
  }
}
