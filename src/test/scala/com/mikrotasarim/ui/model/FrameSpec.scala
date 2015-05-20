package com.mikrotasarim.ui.model

import org.scalatest.{Matchers, FlatSpec}

import scala.util.Random

class FrameSpec extends FlatSpec with Matchers {

  val blackData = Array.fill[Int](384*288)(0)
  val grayData = Array.fill[Int](384*288)(0x1fff)
  val whiteData = Array.fill[Int](384*288)(0x3fff)
  
  val blackFrame = Frame.createFrom14Bit(384, 288, blackData)
  val grayFrame = Frame.createFrom14Bit(384, 288, grayData)
  val whiteFrame = Frame.createFrom14Bit(384, 288, whiteData)
  
  val diagonalData = Array.ofDim[Int](384*288)
  for (i <- 0 until 384) for (j <- 0 until 288) diagonalData(j * 384 + i) = 8192 * i / 383 + 8192 * j / 287
  val diagonalFrame = Frame.createFrom14Bit(384, 288, diagonalData)

  val randomData = Array.ofDim[Int](384*288)
  for (i <- randomData.indices) randomData(i) = Random.nextInt(16383)
  val randomFrame = Frame.createFrom14Bit(384, 288, randomData)

  "A frame" should "do equalization as defined" in {
    val frameData = Array.ofDim[Int](384*288)
    for (i <- frameData.indices) frameData(i)=i
    val frame = new Frame(384, 288, frameData, 16384)
    val equalized = frame.topBotCut(16384)
//    for (i <- 0 until equalized.data.length) println(equalized.data(i))
  }

  it should "be saved as grayscale image" in {
    Frame.save(blackFrame.getGrayscale, "a.tif")
    Frame.show16Bit("./a.tif")
    Frame.save(grayFrame.getGrayscale, "b.tif")
    Frame.show16Bit("./b.tif")
    Frame.save(whiteFrame.getGrayscale, "c.tif")
    Frame.show16Bit("./c.tif")
    Frame.save(diagonalFrame.getGrayscale, "d.tif")
    Frame.show16Bit("./d.tif")
    Frame.save(randomFrame.getGrayscale, "e.tif")
    Frame.show16Bit("./e.tif")
  }

  it should "be saved as thermographic image" in {
    Frame.save(blackFrame.getHeatmap, "ac.tif")
    Frame.show16Bit("./ac.tif")
    Frame.save(grayFrame.getHeatmap, "bc.tif")
    Frame.show16Bit("./bc.tif")
    Frame.save(whiteFrame.getHeatmap, "cc.tif")
    Frame.show16Bit("./cc.tif")
    Frame.save(diagonalFrame.getHeatmap, "dc.tif")
    Frame.show16Bit("./dc.tif")
    Frame.save(randomFrame.getHeatmap, "ec.tif")
    Frame.show16Bit("./ec.tif")
  }
  
  it should "create a histogram" in {
    val histogram = diagonalFrame.withDepth(128).histogram()
    for (i <- histogram.zipWithIndex) {
      println(i._2 + " -> " + i._1)
    }
  }
}
