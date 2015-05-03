package com.mikrotasarim.ui.model

import java.io.File

import com.fasterxml.jackson.annotation.JsonProperty
import com.lambdaworks.jacks.JacksMapper
import org.apache.commons.io.FileUtils

class Measurement {

  @JsonProperty("name")
  private var _name: String = ""

  def name = _name
  def name_=(that: String) = _name = that

  @JsonProperty("netd")
  val netd = Array.ofDim[Int](384 * 288)

  @JsonProperty("dead")
  val dead = Array.ofDim[Boolean](384 * 288)

  @JsonProperty("dark")
  private var _dark = Array.ofDim[Int](384 * 288)

  def dark = _dark
  def dark_=(that: Array[Int]) = _dark = that

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
      that.dark.sameElements(this.dark)
    case _ => false
  }
  override def hashCode = name.toUpperCase.hashCode
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
}