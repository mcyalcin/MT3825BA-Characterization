package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.command.ApiConstants.{NucMode, TriggerMode}
import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.api.device.{OpalKellyInterface, ConsoleMockDeviceInterface}

import scalafx.beans.property.{BooleanProperty, StringProperty}

object FpgaController {

  var deviceController: DeviceController = null
  var device: OpalKellyInterface = null

  val deviceConnected = BooleanProperty(value = false)
  val isSelfTest = BooleanProperty(value = false)

  val xOrigin = StringProperty("0")
  val xSize = StringProperty("384")
  val yOrigin = StringProperty("0")
  val ySize = StringProperty("290")
  // TODO: Add validation to number fields

  xOrigin.onChange(deviceController.setWindowOrigin(xOrigin.value.toLong, yOrigin.value.toLong))
  yOrigin.onChange(deviceController.setWindowOrigin(xOrigin.value.toLong, yOrigin.value.toLong))
  xSize.onChange(deviceController.setWindowSize(xSize.value.toLong, ySize.value.toLong))
  ySize.onChange(deviceController.setWindowSize(xSize.value.toLong, ySize.value.toLong))

  def resetWindowSize(): Unit = {
    xOrigin.set("0")
    xSize.set("384")
    yOrigin.set("0")
    ySize.set("290")
  }

  def connectToFpga(): Unit = {
    deviceController = if (!isSelfTest.value) {
      if (device == null) {
        device = new OpalKellyInterface("guy.bit")
      }
      new DeviceController(device)
    } else {
      new DeviceController(new ConsoleMockDeviceInterface)
    }
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
