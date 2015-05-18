package com.mikrotasarim.ui.model

import com.mikrotasarim.image.FrameImage
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
    Frame.save(frame.getGrayscale, "a.tif")
    FrameImage.show("./a.tif")
    Frame.save(frame2.getGrayscale, "b.tif")
    FrameImage.show("./b.tif")
    Frame.save(frame3.getGrayscale, "c.tif")
    FrameImage.show("./c.tif")
  }

  it should "be saved as thermographic image" in {
    val blackImage = Array.ofDim[Int](384 * 288)
    val frame = new Frame(384, 288, blackImage, 16384)
    val frame2 = new Frame(384, 288, blackImage.map(a => 0x1fff), 16384)
    val frame3 = new Frame(384, 288, blackImage.map(a => 0x3fff), 16384)
    Frame.save(frame.getThermo, "ac.tif")
    FrameImage.show("./ac.tif")
    Frame.save(frame2.getThermo, "bc.tif")
    FrameImage.show("./bc.tif")
    Frame.save(frame3.getThermo, "cc.tif")
    FrameImage.show("./cc.tif")
  }
}
