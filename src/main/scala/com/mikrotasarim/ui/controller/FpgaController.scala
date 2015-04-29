package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.command.ApiConstants.{NucMode, TriggerMode}
import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.api.device.{OpalKellyInterface, ConsoleMockDeviceInterface}

import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.{ObservableBuffer, ObservableArray}

object FpgaController {

  var deviceController: DeviceController = null
  var device: OpalKellyInterface = null

  val deviceConnected = BooleanProperty(value = false)
  val isSelfTest = BooleanProperty(value = false)
  val correctionEnabled = BooleanProperty(value = false)
  val onePointCorrection = BooleanProperty(value = false)
  val twoPointCorrection = BooleanProperty(value = false)

  val flashPartitions = ObservableBuffer(List(
      "Partition 1",
      "Partition 2",
      "Partition 3",
      "Partition 4",
      "Partition 5",
      "Partition 6",
      "Partition 7",
      "Partition 8",
      "Partition 9",
      "Partition 10",
      "Partition 11",
      "Partition 12"
    )
  )
  val selectedPartition = StringProperty("Partition 1")

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

  def calculateAndApplyNuc(): Unit = {
    val nucCalibrationDistances = for (i <- 0 to 63) yield {
      deviceController.setNucMode(NucMode.Fixed, i)
      val frameSet = for (i <- 0 until 10) yield {
        val rawFrame = deviceController.getFrame
        for (i <- 0 until 384 * 288) yield {
          rawFrame(2 * i) + rawFrame(2 * i + 1) * 256
        }
      }
      for (i <- 0 until 384 * 288) yield
        math.abs((for (j <- 0 until 10) yield frameSet(j)(i)).sum.toDouble / 10 - 8192)
    }
    val idealNuc = for (i <- 0 until 384 * 288) yield {
      var min = nucCalibrationDistances(0)(i)
      var minIndex = 0
      for (j <- 1 until 63) {
        if (nucCalibrationDistances(j)(i) < min) {
          min = nucCalibrationDistances(j)(i)
          minIndex = j
        }
      }
      minIndex
    }.toByte
    deviceController.writeFrameToFlashMemory(idealNuc)
    deviceController.setNucMode(NucMode.Enabled)
  }

  def disconnectFromFpga(): Unit = {
    deviceConnected.set(false)
  }
}
