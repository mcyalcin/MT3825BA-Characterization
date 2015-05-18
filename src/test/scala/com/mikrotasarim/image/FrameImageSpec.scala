package com.mikrotasarim.image

import org.scalatest.{Matchers, FlatSpec}

class FrameImageSpec extends FlatSpec with Matchers {

  "A frame" should "be saved" in {
    val blackImage = Array.ofDim[Int](384 * 288)
    val frame = new FrameImage(blackImage)
    val frame2 = new FrameImage(blackImage.map(a => 0xafff))
    val frame3 = new FrameImage(blackImage.map(a => 0xffff))
    frame.saveTiff("a.tiff")
    FrameImage.show("./a.tiff")
    frame2.saveTiff("b.tiff")
    FrameImage.show("./b.tiff")
    frame3.saveTiff("c.tiff")
    FrameImage.show("./c.tiff")
  }

  it should "be created from raw data" in {
    val blackImageRaw = Array.ofDim[Byte](384 * 288 * 2)
    val frame = FrameImage.fromRaw(blackImageRaw)
    frame.saveTiff("r.tiff")
    FrameImage.show("./r.tiff")
  }
}
