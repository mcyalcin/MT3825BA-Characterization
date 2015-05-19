package com.mikrotasarim.ui.model

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import ij.{IJ, ImagePlus}

object Frame {
  def createFromRaw(xSize: Int, ySize: Int, rawData: Seq[Byte], depth: Int): Frame = {
    def unsigned(b: Byte): Int = {
      (b.toInt + 256) % 256
    }
    val data = for (i <- 0 until 384 * 288) yield unsigned(rawData(2 * i)) + unsigned(rawData(2 * i + 1)) * 256
    createFrom14Bit(xSize, ySize, data)
  }

  def createFrom14Bit(xSize: Int, ySize: Int, data: Seq[Int]): Frame = {
    val depth = 16384
    new Frame(xSize, ySize, data.map(_*4), depth * 4)
  }

  def save(image: BufferedImage, file: File): Unit = {
    ImageIO.write(image, "TIFF", file)
  }

  def save(image: BufferedImage, fileName: String): Unit = {
    val file = new File(fileName)
    save(image, file)
  }

  def show16Bit(fileName: String): Unit = {
    val img: ImagePlus = IJ.openImage(fileName)
    img.show()
  }

  def show14Bit(fileName: String): Unit = {
    val img: ImagePlus = IJ.openImage(fileName)
    img.show()
    IJ.setMinAndMax(0, 16383)
  }
}

class Frame(val xSize: Int, val ySize: Int, val data: Seq[Int], val depth: Int) {

  require(data.length == xSize * ySize)

  def histogram(): IndexedSeq[Int] = {
    val hist = Array.ofDim[Int](depth)
    for (i <- 0 until data.length) {
      hist(data(i)) += 1
    }
    hist
  }

  def histogramData(min: Int, max: Int, steps: Int): Unit = {
    // TODO: Return type should be data binding value for a scalafx chart
  }

  def getThermo: BufferedImage = {
    val image = new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_RGB)
    val rgb = Array.ofDim[Int](data.length)
    val cut = depth / 4
    for (i <- 0 until data.length) {
      val pixel = data(i)
      if (pixel < 0) {
        println("wtf")
        val unsigned = pixel + depth
        if (unsigned < 3 * cut) {
          val red = 255 * (unsigned - 2 * cut + 1) / cut
          val green = 255
          val blue = 0
          rgb(i) = red * 256 * 256 + green * 256 + blue
        } else {
          val red = 255
          val green = 255 - (255 * (unsigned - 3 * cut + 1) / cut)
          val blue = 0
          rgb(i) = red * 256 * 256 + green * 256 + blue
        }
      } else if (pixel < cut) {
        val red = 0
        val green = 255 * pixel / (cut - 1)
        val blue = 255
        rgb(i) = red * 256 * 256 + green * 256 + blue
      } else if (pixel < 2 * cut) {
        val red = 0
        val green = 255
        val blue = 255 - (255 * (pixel - cut + 1) / cut)
        rgb(i) = red * 256 * 256 + green * 256 + blue
      } else if (pixel < 3 * cut) {
        val red = 255 * (pixel - 2 * cut + 1) / cut
        val green = 255
        val blue = 0
        rgb(i) = red * 256 * 256 + green * 256 + blue
      } else {
        val red = 255
        val green = 255 - (255 * (pixel - 3 * cut + 1) / cut)
        val blue = 0
        rgb(i) = red * 256 * 256 + green * 256 + blue
      }
    }
    image.getRaster.setDataElements(0, 0, xSize, ySize, rgb)
    image
  }

  def getGrayscale: BufferedImage = {
    val image = new BufferedImage(xSize, ySize, BufferedImage.TYPE_USHORT_GRAY)
    val shortData = data.map(_.toShort).toArray
    image.getRaster.setDataElements(0, 0, xSize, ySize, shortData)
    image
  }

  def save(fileName: String): Unit = {
    Frame.save(getGrayscale, fileName)
  }

  def topBotCut(): Frame = {
    topBotCut(depth)
  }

  def topBotCut(targetDepth: Int): Frame = {
    val sorted = data.sorted
    val min = sorted(sorted.length / 10)
    val max = sorted(9 * sorted.length / 10)
    val equalizedData = data.map(i =>
      if (i >= max) targetDepth - 1
      else if (i <= min) 0
      else (i - min) * targetDepth / (max - min)
    )
    new Frame(xSize, ySize, equalizedData, targetDepth)
  }

  def withDepth(targetDepth: Int): Frame = {
    val mappedData = data.map(i => i * (targetDepth - 1) / (depth-1))
    new Frame(xSize, ySize, mappedData, targetDepth)
  }
}
