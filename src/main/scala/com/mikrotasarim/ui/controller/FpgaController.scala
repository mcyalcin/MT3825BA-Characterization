package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.NucFrame
import com.mikrotasarim.api.command.ApiConstants.{NucMode, TriggerMode}
import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.api.device.{ConsoleMockDeviceInterface, OpalKellyInterface}

import scalafx.beans.property.{BooleanProperty, StringProperty}
import scalafx.collections.ObservableBuffer

object FpgaController {

  // TODO: Use of null is ugly. Replace with options, doing the necessary rework everywhere these are used.
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

  selectedPartition.onChange(
    currentNucLabel.value = nucFrames(partitionToIndex(selectedPartition.value)).getOrElse(new NucFrame("", null, null)).name
  )

  val partitionToIndex = Map(
    "Partition 1" -> 0,
    "Partition 2" -> 1,
    "Partition 3" -> 2,
    "Partition 4" -> 3,
    "Partition 5" -> 4,
    "Partition 6" -> 5,
    "Partition 7" -> 6,
    "Partition 8" -> 7,
    "Partition 9" -> 8,
    "Partition 10" -> 9,
    "Partition 11" -> 10,
    "Partition 12" -> 11
  )

  val nucFrames = Array.ofDim[Option[NucFrame]](12)
  for (i <- 0 until 12) nucFrames(i) = None
  val nucLabel = StringProperty("")
  val currentNucLabel = StringProperty("")

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
        device = new OpalKellyInterface("guy.bit")
      }
      new DeviceController(device)
    } else {
      new DeviceController(new ConsoleMockDeviceInterface)
    }
    deviceController.takeFpgaOffReset()
    deviceController.initializeRoic()
    deviceController.setTriggerMode(TriggerMode.Slave_Software)
    deviceController.setNucMode(NucMode.Enabled)
    deviceController.enableImagingMode()
    deviceConnected.set(true)
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
    val deadPixels = Array.ofDim[Boolean](384*288)
    val idealNuc = for (i <- 0 until 384 * 288) yield {
      var min = nucCalibrationDistances(0)(i)
      var minIndex = 0
      for (j <- 1 until 63) {
        if (nucCalibrationDistances(j)(i) < min) {
          min = nucCalibrationDistances(j)(i)
          minIndex = j
        }
      }
      if (min > 1000) deadPixels(i) = true
      minIndex
    }.toByte
    val frame = Array.ofDim[Byte](288, 384)
    for (i <- 0 until 288 * 384) {
      frame(i / 384)(i % 384) = idealNuc(i)
    }
    deviceController.writeFrameToFlashMemory(frame)
    deviceController.setNucMode(NucMode.Enabled)
    nucFrames(partitionToIndex(selectedPartition.value)) = Some(new NucFrame(nucLabel.value, frame, deadPixels))
    currentNucLabel.value = nucLabel.value
    nucLabel.value = ""
  }

  def disconnectFromFpga(): Unit = {
    deviceController.putFpgaOnReset()
    deviceConnected.set(false)
  }
}
