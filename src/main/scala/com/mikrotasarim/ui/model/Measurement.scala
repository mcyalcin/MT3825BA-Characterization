package com.mikrotasarim.ui.model

import java.io.File

import com.fasterxml.jackson.annotation.JsonProperty
import com.lambdaworks.jacks.JacksMapper
import com.mikrotasarim.ui.controller.MeasurementController
import org.apache.commons.io.FileUtils

import scalafx.beans.property.{StringProperty, BooleanProperty}

class Measurement {
  @JsonProperty
  private var _referenceResistorMap: Array[Double] = Array.ofDim[Double](384*12)

  def referenceResistorMap: Array[Double] = _referenceResistorMap

  def referenceResistorMap_=(that: Array[Double]) = _referenceResistorMap = that

  @JsonProperty
  private var _resistorMap: Array[Double] = Array.ofDim[Double](384 * 288)

  def resistorMap = _resistorMap

  def resistorMap_=(that: Array[Double]) = _resistorMap = that

  @JsonProperty
  private var _responsivity: Array[Double] = Array.ofDim[Double](384 * 288)

  def responsivity = _responsivity

  def responsivity_=(that: Array[Double]) = _responsivity = that

  @JsonProperty
  private var _temp0: Double = 0

  def temp0 = _temp0

  def temp0_=(that: Double) = _temp0 = that

  @JsonProperty
  var _netdDevsT0: Array[Double] = Array.ofDim[Double](384 * 288)

  def netdDevsT0 = _netdDevsT0

  def netdDevsT0_=(that: Array[Double]) = _netdDevsT0 = that

  @JsonProperty
  private var _netdMeansT0: Array[Double] = Array.ofDim[Double](384 * 288)

  def netdMeansT0 = _netdMeansT0

  def netdMeansT0_=(that: Array[Double]) = _netdMeansT0 = that

  @JsonProperty
  private var _temp1: Double = 0

  def temp1 = _temp1

  def temp1_=(that: Double) = _temp1 = that

  @JsonProperty
  var _netdDevsT1: Array[Double] = Array.ofDim[Double](384 * 288)

  def netdDevsT1 = _netdDevsT1

  def netdDevsT1_=(that: Array[Double]) = _netdDevsT1 = that

  @JsonProperty
  private var _netdMeansT1: Array[Double] = Array.ofDim[Double](384 * 288)

  def netdMeansT1 = _netdMeansT1

  def netdMeansT1_=(that: Array[Double]) = _netdMeansT1 = that

  def calculateSlope(): Unit = {
    if (graySet && darkSet) {
      val avg = (for (i <- 0 until 384 * 288) yield Seq(gray(i) - dark(i), 0).max).sum.toDouble / (384 * 288)
      // TODO: Implement rejection rules and dead pixel marking here if necessary
      //      if (avg < 100) {
      //        slopeSet = false
      //        throw new Exception("Gray and Dark images are too close to calculate slope.")
      //      }
      slope = (for (i <- 0 until 384 * 288) yield avg / (gray(i) - dark(i)).toDouble).toArray
      slopeSet = true
    }
  }

  @JsonProperty("name")
  private var _name: String = ""

  def name = _name

  def name_=(that: String) = _name = that

  @JsonProperty("netd0")
  private var _netd0 = Array.ofDim[Double](384 * 288)

  def netd0 = _netd0

  def netd0_=(that: Array[Double]) = _netd0 = that

  @JsonProperty("netd1")
  private var _netd1 = Array.ofDim[Double](384 * 288)

  def netd1 = _netd1

  def netd1_=(that: Array[Double]) = _netd1 = that

  @JsonProperty("dead")
  private var _dead = Array.ofDim[Boolean](384 * 288)

  def dead = _dead

  def dead_=(that: Array[Boolean]) = _dead = that

  @JsonProperty("dark")
  private var _dark = Array.ofDim[Int](384 * 288)

  def dark = _dark

  def dark_=(that: Array[Int]) = _dark = that

  @JsonProperty("darkSet")
  private var _darkSet = false

  def darkSet = _darkSet

  def darkSet_=(that: Boolean) = {
    _darkSet = that
    Measurement.darkImageSet.set(that)
  }

  @JsonProperty("gray")
  private var _gray = Array.ofDim[Int](384 * 288)

  def gray = _gray

  def gray_=(that: Array[Int]) = _gray = that

  @JsonProperty("graySet")
  private var _graySet = false

  def graySet = _graySet

  def graySet_=(that: Boolean) = _graySet = that

  @JsonProperty("slope")
  private var _slope = Array.ofDim[Double](384 * 288)

  def slope = _slope

  def slope_=(that: Array[Double]) = _slope = that

  @JsonProperty("slopeSet")
  private var _slopeSet = false

  def slopeSet = _slopeSet

  def slopeSet_=(that: Boolean) = {
    _slopeSet = that
    Measurement.slopeSet.set(that)
  }

  @JsonProperty("noise")
  private var _noise = Array.ofDim[Double](384 * 288)

  def noise = _noise

  def noise_=(that: Array[Double]) = _noise = that

  def save(fileName: String): Unit = {
    val file = new File(fileName)
    save(file)
  }

  def save(file: File): Unit = {
    def divideToLines(source: Array[Double]): Array[Array[Double]] = {
      val result = Array.ofDim[Array[Double]](288)
      for (i <- 0 until 288) {
        result(i) = source.drop(i * 384).take(384)
      }
      result
    }
    val json = JacksMapper.writeValueAsString[Measurement](this)
    val noiseCsv = JacksMapper.writeValueAsString[Array[Array[Double]]](divideToLines(this.noise)).replaceAll("]", "\n").replaceAll("\\[", "").replaceAll("]", "")
    val netd0Csv = JacksMapper.writeValueAsString[Array[Array[Double]]](divideToLines(this.netd0)).replaceAll("]", "\n").replaceAll("\\[", "").replaceAll("]", "")
    val netd1Csv = JacksMapper.writeValueAsString[Array[Array[Double]]](divideToLines(this.netd1)).replaceAll("]", "\n").replaceAll("\\[", "").replaceAll("]", "")
    val resistorMapCsv = JacksMapper.writeValueAsString[Array[Array[Double]]](divideToLines(this.resistorMap)).replaceAll("],", "\n").replaceAll("\\[", "").replaceAll("]", "")
    val referenceResistorMapCsv = JacksMapper.writeValueAsString[Array[Array[Double]]](divideToLines(this.referenceResistorMap)).replaceAll("],", "\n").replaceAll("\\[", "").replaceAll("]", "")
    val responsivityCsv = JacksMapper.writeValueAsString[Array[Array[Double]]](divideToLines(this.responsivity)).replaceAll("]", "\n").replaceAll("\\[", "").replaceAll("]", "")
    val dead = JacksMapper.writeValueAsString[Array[Array[Double]]](divideToLines(this.dead.map(b => if (b) 1.0 else 0.0))).replaceAll("]", "\n").replaceAll("\\[", "").replaceAll("]", "")

    FileUtils.write(file, json, "UTF-8")
    val blaFile = new File(name + File.separator + "bla.csv")
    blaFile.getParentFile.mkdir
    FileUtils.write(new File(name + File.separator + "noise.csv"), noiseCsv, "UTF-8")
    FileUtils.write(new File(name + File.separator + "netd0.csv"), netd0Csv, "UTF-8")
    FileUtils.write(new File(name + File.separator + "netd1.csv"), netd1Csv, "UTF-8")
    FileUtils.write(new File(name + File.separator + "resistorMap.csv"), resistorMapCsv, "UTF-8")
    FileUtils.write(new File(name + File.separator + "referenceResistorMap.csv"), referenceResistorMapCsv, "UTF-8")
    FileUtils.write(new File(name + File.separator + "responsivity.csv"), responsivityCsv, "UTF-8")
    FileUtils.write(new File(name + File.separator + "deadPixels.csv"), dead, "UTF-8")
  }

  override def equals(o: Any) = o match {
    case that: Measurement =>
      that.name.equalsIgnoreCase(this.name) &&
        that.netd0.sameElements(this.netd0) &&
        that.dead.sameElements(this.dead) &&
        that.dark.sameElements(this.dark) &&
        that.slope.sameElements(this.slope)
    case _ => false
  }

  // TODO: Implement correctly.

  override def hashCode = name.toUpperCase.hashCode

  // TODO: Implement a better hashCode
}

object Measurement {

  def fromFile(fileName: String): Measurement = {
    val file = new File(fileName)
    fromFile(file)
  }

  def fromFile(file: File): Measurement = {
    val json = FileUtils.readFileToString(file, "UTF-8")
    JacksMapper.readValue[Measurement](json)
  }

  // TODO: Move these to MeasurementController. Let only the persistence methods remain here.
  val darkImageSet = BooleanProperty(value = false)
  val slopeSet = BooleanProperty(value = false)
  val name = StringProperty("")

  name.onChange(MeasurementController.measurement.name = name.value)
}