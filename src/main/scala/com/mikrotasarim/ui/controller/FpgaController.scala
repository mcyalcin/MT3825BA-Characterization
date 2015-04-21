package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.command.ApiConstants.{NucMode, TriggerMode}
import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.api.device.{OpalKellyInterface, ConsoleMockDeviceInterface}

import scalafx.beans.property.{BooleanProperty, StringProperty}

object FpgaController {

  var deviceController = new DeviceController(new ConsoleMockDeviceInterface)

  val deviceConnected = BooleanProperty(value = false)

  val xOrigin = StringProperty("0")
  val xSize = StringProperty("384")
  val yOrigin = StringProperty("0")
  val ySize = StringProperty("290")

  def resetWindowSize(): Unit = {
    xOrigin.set("0")
    xSize.set("384")
    yOrigin.set("0")
    ySize.set("290")
  }

  def connectToFpga(): Unit = {
    val device = new OpalKellyInterface("guy.bit")
    deviceController = new DeviceController(device)
    deviceController.initializeRoic()
    deviceController.setTriggerMode(TriggerMode.Slave_Software)
    deviceController.setNucMode(NucMode.Enabled)
    deviceController.enableImagingMode()
    deviceConnected.set(true)

    // TODO: Add exception handling
  }

  def disconnectFromFpga(): Unit = {
    deviceConnected.set(false)
  }
}
