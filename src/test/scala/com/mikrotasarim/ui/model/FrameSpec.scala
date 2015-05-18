package com.mikrotasarim.ui.model

import org.scalatest.{Matchers, FlatSpec}

class FrameSpec extends FlatSpec with Matchers {

  "A frame" should "do equalization as defined" in {
    val frameData = Array.ofDim[Int](384*288)
    for (i <- 0 until frameData.length) frameData(i)=i
    val frame = new Frame(384, 288, frameData, 16384)
    val equalized = frame.equalized(16384)
    for (i <- 0 until equalized.data.length) println(equalized.data(i))
  }
}
