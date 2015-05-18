package com.mikrotasarim.ui.model

import com.mikrotasarim.image.FrameImage

class Frame(val xSize: Int, val ySize: Int, val rawData: Seq[Byte]) {

  if (rawData.length != xSize * ySize * 2) throw new Exception("Window size does not match the captured data.")

  val data: Seq[Int] = {
    def unsigned(b: Byte): Int = {
      (b.toInt + 256) % 256
    }
    for (i <- 0 until 384 * 288) yield unsigned(rawData(2 * i)) + unsigned(rawData(2 * i + 1)) * 256
  }

  def save(fileName: String): Unit = {
    FrameImage.saveTiff(xSize, ySize, data.toArray, fileName)
  }

  def histogram(): Unit = {
    // TODO
  }

  def heatMap(): Unit = {
    // TODO
  }

  def grayScale(): Unit = {
    // TODO
  }

  def equalized(): Frame = {
    // TODO
    this
  }
}
