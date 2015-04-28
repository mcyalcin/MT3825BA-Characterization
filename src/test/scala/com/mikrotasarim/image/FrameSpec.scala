package com.mikrotasarim.image

import org.scalatest.{Matchers, FlatSpec}

class FrameSpec extends FlatSpec with Matchers{

  "A frame" should "be saved" in {
    val blackImage = Array.ofDim[Int](384*288)
    val frame = new Frame(blackImage)
    val frame2 = new Frame(blackImage.map(a => 0xafff))
    val frame3 = new Frame(blackImage.map(a => 0xffff))
    frame.saveTiff("a.tiff")
    Frame.show("a.tiff")
    frame2.saveTiff("b.tiff")
    Frame.show("b.tiff")
    frame3.saveTiff("c.tiff")
    Frame.show("c.tiff")
  }

  it should "be created from raw data" in {
    val blackImageRaw = Array.ofDim[Byte](384*288*2)
    val frame = Frame.fromRaw(blackImageRaw)
    frame.saveTiff("r.tiff")
    Frame.show("r.tiff")
  }
}
