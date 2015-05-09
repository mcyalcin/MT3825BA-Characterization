package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.command.ApiConstants.{NucMode, TriggerMode}
import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.api.device.{ConsoleMockDeviceInterface, OpalKellyInterface}

import scalafx.beans.property.{BooleanProperty, StringProperty}

object FpgaController {

  // TODO: Use of null is ugly. Replace with options, doing the necessary rework everywhere these are used.
  var deviceController: DeviceController = null
  var device: OpalKellyInterface = null

  val deviceConnected = BooleanProperty(value = false)
  val isSelfTest = BooleanProperty(value = false)
  val isCmosTest = BooleanProperty(value = false)
  val correctionEnabled = BooleanProperty(value = false)
  val onePointCorrection = BooleanProperty(value = false)
  val twoPointCorrection = BooleanProperty(value = false)

  val xOrigin = StringProperty("0")
  val xSize = StringProperty("384")
  val yOrigin = StringProperty("0")
  val ySize = StringProperty("288")
  // TODO: Add validation to number fields

  xOrigin.onChange(deviceController.setWindowOrigin(xOrigin.value.toLong, yOrigin.value.toLong))
  yOrigin.onChange(deviceController.setWindowOrigin(xOrigin.value.toLong, yOrigin.value.toLong))
  xSize.onChange(deviceController.setWindowSize(xSize.value.toLong, ySize.value.toLong))
  ySize.onChange(deviceController.setWindowSize(xSize.value.toLong, ySize.value.toLong))

  def resetWindowSize(): Unit = {
    xOrigin.set("0")
    xSize.set("384")
    yOrigin.set("0")
    ySize.set("288")
  }

  def connectToFpga(): Unit = {
    deviceController = if (!isSelfTest.value) {
      if (device == null) {
        device = new OpalKellyInterface("D:\\rfi_test_real_flash_package_11.bit")
      }
      new DeviceController(device)
    } else {
      new DeviceController(new ConsoleMockDeviceInterface)
    }
    deviceController.takeFpgaOffReset()
    deviceController.setReset()
    deviceController.clearReset()
    deviceController.disableImagingMode()
    deviceController.initializeRoic()
    deviceController.setNucMode(NucMode.Fixed,255)
    deviceController.sendReferenceDataToRoic()
    deviceController.setTriggerMode(TriggerMode.Slave_Software)
    deviceController.setNucMode(NucMode.Enabled)
    deviceController.setAdcDelay(2)
    deviceController.writeToRoicMemory(22,2047)
    deviceController.writeToRoicMemory(18,4)
    deviceController.setGlobalReferenceBias(3500)
    deviceController.setSamplingDelay(4)
    deviceController.enableImagingMode()
    deviceConnected.set(true)
  }

  isCmosTest.onChange({
    if (isCmosTest.value) {
      deviceController.disableImagingMode()
      deviceController.writeToRoicMemory(17,3)
      deviceController.enableImagingMode()
    } else {
      deviceController.disableImagingMode()
      deviceController.writeToRoicMemory(17,0)
      deviceController.enableImagingMode()
    }
  })

  def disconnectFromFpga(): Unit = {
    deviceController.putFpgaOnReset()
    deviceConnected.set(false)
  }
}
