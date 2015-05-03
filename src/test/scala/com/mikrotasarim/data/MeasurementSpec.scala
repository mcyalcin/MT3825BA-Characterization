package com.mikrotasarim.data

import java.io.File

import org.scalatest.{FlatSpec, Matchers}

class MeasurementSpec extends FlatSpec with Matchers {

  "A measurement" should "be saved" in {
    val m = new Measurement()
    m.save("a.file")
    val file = new File("a.file")
  }

  "A measurement" should "be restored from save file" in {
    val m = new Measurement()
    m.save("a.file")
    val n = Measurement.fromFile("a.file")
    assert(m equals n)
  }
}
