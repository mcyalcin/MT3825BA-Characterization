package com.mikrotasarim.data

import java.io.File

import com.lambdaworks.jacks.JacksMapper
import org.apache.commons.io.FileUtils

import scalafx.beans.property.StringProperty

class Measurement() {

  val name = StringProperty("")

  def save(fileName: String): Unit = {
    val file = new File(fileName)
    save(file)
  }

  def save(file: File): Unit = {
    val json = JacksMapper.writeValueAsString[Measurement](this)
    FileUtils.write(file, json, "UTF-8")
  }

  override def equals(o: Any) = o match {
    case that: Measurement => that.name.value.equalsIgnoreCase(this.name.value)
    case _ => false
  }
  override def hashCode = name.value.toUpperCase.hashCode
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