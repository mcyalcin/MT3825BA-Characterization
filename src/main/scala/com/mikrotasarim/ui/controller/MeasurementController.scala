package com.mikrotasarim.ui.controller

import com.mikrotasarim.data.Measurement

import scalafx.beans.property.StringProperty

object MeasurementController {

  var measurement: Measurement = new Measurement()

  val fileName = StringProperty("")

  def save(): Unit = {
    measurement.save(fileName.value)
  }

  def load(): Unit = {
    measurement = Measurement.fromFile(fileName.value)
    // TODO: Initialize properties using measurement if necessary
  }
}
