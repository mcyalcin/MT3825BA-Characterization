package com.mikrotasarim.image

import ij._

class Frame(val pixelArray: Array[Long]) {

}

object Frame {
  def main(args: Array[String]): Unit = {
    IJ.log("Hell!")
    val img = IJ.openImage("/media/mcyalcin/Data/bla.jpg")
    img.show()

    val image = new ImagePlus()
  }
}
