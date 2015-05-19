package com.mikrotasarim.ui.model

import org.scalatest.{Matchers, FlatSpec}

class FrameSpec extends FlatSpec with Matchers {

  "A frame" should "do equalization as defined" in {
    val frameData = Array.ofDim[Int](384*288)
    for (i <- 0 until frameData.length) frameData(i)=i
    val frame = new Frame(384, 288, frameData, 16384)
    val equalized = frame.topBotCut(16384)
//    for (i <- 0 until equalized.data.length) println(equalized.data(i))
  }

  it should "be saved as grayscale image" in {
    val blackImage = Array.ofDim[Int](384 * 288)
    val frame = Frame.createFrom14Bit(384, 288, blackImage)
    val frame2 = Frame.createFrom14Bit(384, 288, blackImage.map(a => 0x1fff))
    val frame3 = Frame.createFrom14Bit(384, 288, blackImage.map(a => 0x3fff))
    for (i <- 0 until 384) for (j <- 0 until 288) blackImage(j * 384 + i) = 8192 * i / 383 + 8192 * j / 287
    val frame4 = Frame.createFrom14Bit(384, 288, blackImage)
    Frame.save(frame.getGrayscale, "a.tif")
    Frame.show16Bit("./a.tif")
    Frame.save(frame2.getGrayscale, "b.tif")
    Frame.show16Bit("./b.tif")
    Frame.save(frame3.getGrayscale, "c.tif")
    Frame.show16Bit("./c.tif")
    Frame.save(frame4.withDepth(65536).getGrayscale, "d.tif")
    Frame.show16Bit("./d.tif")
  }

  it should "be saved as thermographic image" in {
    val blackImage = Array.ofDim[Int](384 * 288)
    val frame = Frame.createFrom14Bit(384, 288, blackImage)
    val frame2 = Frame.createFrom14Bit(384, 288, blackImage.map(a => 0x1fff))
    val frame3 = Frame.createFrom14Bit(384, 288, blackImage.map(a => 0x3fff))
    for (i <- 0 until 384) for (j <- 0 until 288) blackImage(j * 384 + i) = 8192 * i / 383 + 8192 * j / 287
    val frame4 = Frame.createFrom14Bit(384, 288, blackImage)
    Frame.save(frame.getThermo, "ac.tif")
    Frame.show16Bit("./ac.tif")
    Frame.save(frame2.getThermo, "bc.tif")
    Frame.show16Bit("./bc.tif")
    Frame.save(frame3.getThermo, "cc.tif")
    Frame.show16Bit("./cc.tif")
    Frame.save(frame4.withDepth(65536).getThermo, "dc.tif")
    Frame.show16Bit("./dc.tif")
  }
}
