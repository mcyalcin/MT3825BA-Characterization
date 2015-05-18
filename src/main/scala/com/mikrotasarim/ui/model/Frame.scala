package com.mikrotasarim.ui.model

import com.mikrotasarim.image.FrameImage

object Frame {
  def createFromRaw(xSize: Int, ySize: Int, rawData: Seq[Byte], depth: Int): Frame = {
    def unsigned(b: Byte): Int = {
      (b.toInt + 256) % 256
    }
    val data = for (i <- 0 until 384 * 288) yield unsigned(rawData(2 * i)) + unsigned(rawData(2 * i + 1)) * 256
    create(xSize, ySize, data, depth)
  }

  def create(xSize: Int, ySize: Int, data: Seq[Int], depth: Int): Frame = {
    new Frame(xSize, ySize, data, depth)
  }
}

class Frame(val xSize: Int, val ySize: Int, val data: Seq[Int], val depth: Int) {

  require(data.length == xSize * ySize)

  def save(fileName: String): Unit = {
    FrameImage.saveTiff(xSize, ySize, data.toArray, fileName)
  }

  def histogram(): IndexedSeq[Int] = {
    val hist = Array.ofDim[Int](depth)
    for (i <- 0 until data.length) {
      hist(data(i)) += 1
    }
    hist
  }

  def histogramData(min: Int, max: Int, steps: Int): Unit = {
    // TODO
  }

  def heatMap(): Unit = {
    // TODO
  }

  def grayScale(): Unit = {
    // TODO
  }

  def equalized(): Frame = {
    equalized(depth)
  }

  def equalized(targetDepth: Int): Frame = {
    val sorted = data.sorted
    val min = sorted(sorted.length / 10 + 1)
    val max = sorted(9 * sorted.length / 10 - 1)
    val equalizedData = data.map(i =>
      if (i > max) targetDepth - 1
      else if (i < min) 0
      else (i - min) * targetDepth / (max - min)
    )
    new Frame(xSize, ySize, equalizedData, targetDepth)
  }
}
