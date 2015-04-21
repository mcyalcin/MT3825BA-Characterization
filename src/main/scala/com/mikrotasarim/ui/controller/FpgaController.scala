package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.api.device.ConsoleMockDeviceInterface

import scalafx.beans.property.StringProperty

object FpgaController {

  var DeviceController = new DeviceController(new ConsoleMockDeviceInterface)

  val xOrigin = StringProperty("0")
  val xSize = StringProperty("384")
  val yOrigin = StringProperty("0")
  val ySize = StringProperty("290")

  def resetWindowSize(): Unit = {
    xOrigin.value = "0"
    xSize.value = "384"
    yOrigin.value = "0"
    ySize.value = "290"
  }

  def connectToFpga(): Unit = {

  }

  def disconnectFromFpga(): Unit = {

  }
}
