package com.mikrotasarim.image

import ij._

class Frame(val pixelArray: Array[Long]) {

}

object Frame {

  def show(): Unit = {
    val img = IJ.openImage("a.png")
    img.show()
  }
}
