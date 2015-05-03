package com.mikrotasarim.ui.model

import java.io.File

import com.fasterxml.jackson.annotation.JsonProperty
import com.lambdaworks.jacks.JacksMapper
import com.mikrotasarim.ui.controller.MeasurementController
import org.apache.commons.io.FileUtils

import scalafx.beans.property.{StringProperty, BooleanProperty}

class Measurement {
  def calculateSlope(): Unit = {
    if (graySet && darkSet) {
      val avg = (for (i <- 0 until 384 * 288) yield Seq(gray(i) - dark(i), 0).max).sum.toDouble / (384*288)
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

  @JsonProperty("netd")
  val netd = Array.ofDim[Int](384 * 288)

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

  def save(fileName: String): Unit = {
    val file = new File(fileName)
    save(file)
  }

  def save(file: File): Unit = {
    val json = JacksMapper.writeValueAsString[Measurement](this)
    FileUtils.write(file, json, "UTF-8")
  }

  override def equals(o: Any) = o match {
    case that: Measurement =>
      that.name.equalsIgnoreCase(this.name) &&
        that.netd.sameElements(this.netd) &&
        that.dead.sameElements(this.dead) &&
        that.dark.sameElements(this.dark) &&
        that.slope.sameElements(this.slope)
    case _ => false
  }

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

  val darkImageSet = BooleanProperty(value = false)
  val slopeSet = BooleanProperty(value = false)
  val name = StringProperty("")

  name.onChange(MeasurementController.measurement.name = name.value)
}