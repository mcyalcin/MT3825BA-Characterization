package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.NucFrame
import com.mikrotasarim.api.command.ApiConstants.NucMode
import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.image.FrameImage

import scalafx.beans.property.{IntegerProperty, StringProperty}
import scalafx.collections.ObservableBuffer

object CalibrationController {
  def resetIntegrationTime(): Unit = {
    integrationTime.set(64)
    applyIntegrationTime()
  }

  def applyIntegrationTime(): Unit = dc.setIntegrationTime(integrationTime.value * 3)

  def resetPixelBiasRange(): Unit = {
    pixelBiasRange.set(55)
    applyPixelBiasRange()
  }

  def applyPixelBiasRange(): Unit = dc.setPixelBiasRange(4096 * pixelBiasRange.value / 1500)

  def resetGlobalReferenceBias(): Unit = {
    globalReferenceBias.set(2563)
    applyGlobalReferenceBias()
  }

  def applyGlobalReferenceBias(): Unit = dc.setGlobalReferenceBias(4096 * globalReferenceBias.value / 3000)

  def resetAdcDelay(): Unit = {
    adcDelay.set(2)
    applyAdcDelay()
  }

  def applyAdcDelay(): Unit = dc.setAdcDelay(adcDelay.value)

  def dc: DeviceController = FpgaController.deviceController

  val globalReferenceBias = IntegerProperty(2563)
  val pixelBiasRange = IntegerProperty(55)
  val integrationTime = IntegerProperty(64)
  val adcDelay = IntegerProperty(2)

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

  selectedPartition.onChange({
    currentNucLabel.value = nucFrames(partitionToIndex(selectedPartition.value)).getOrElse(new NucFrame("", null, null)).name
    dc.disableImagingMode()
    FpgaController.deviceController.setActiveFlashPartition(partitionToIndex(selectedPartition.value))
    FpgaController.deviceController.enableImagingMode()
  })

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

  def calculateAndApplyNuc(): Unit = {
    val dc = FpgaController.deviceController
    val nucCalibrationDistances = for (i <- 0 to 63) yield {
      dc.disableImagingMode()
      dc.setNucMode(NucMode.Fixed, i + 192)
      dc.enableImagingMode()
      val numFrames = 2
      val frameSet = for (i <- 0 until numFrames) yield {
        val rawFrame = dc.getFrame
        for (i <- 0 until 384 * 288) yield {
          rawFrame(2 * i) + rawFrame(2 * i + 1) * 256
        }
      }
      val bas = FrameImage.fromProcessed(frameSet.head.toArray)
      bas.saveTiff("nucFrame_" + i + ".tif")
      for (i <- 0 until 384 * 288) yield
        math.abs((for (j <- 0 until numFrames) yield frameSet(j)(i)).sum.toDouble / numFrames - 8192)
    }
    val deadPixels = Array.ofDim[Boolean](384 * 288)
    val idealNuc = for (i <- 0 until 384 * 288) yield {
      var min = nucCalibrationDistances.head(i)
      var minIndex = 0
      for (j <- 1 to 63) {
        if (nucCalibrationDistances(j)(i) < min) {
          min = nucCalibrationDistances(j)(i)
          minIndex = j
        }
      }
      if (min > 1000) deadPixels(i) = true
      minIndex
    }.toByte
    MeasurementController.measurement.dead = deadPixels
    val nucFrame = FrameImage.fromProcessed(idealNuc.map(_.toInt).toArray)
    nucFrame.saveTiff("nucFrame.tif")
    val frame = Array.ofDim[Byte](288, 384)
    for (i <- 0 until 288 * 384) {
      frame(i / 384)(i % 384) = (idealNuc(i) + 192).toByte
    }
    dc.disableImagingMode()
    dc.eraseActiveFlashPartition()
    dc.writeFrameToFlashMemory(frame)
    dc.setNucMode(NucMode.Enabled)
    dc.enableImagingMode()
    nucFrames(partitionToIndex(selectedPartition.value)) = Some(new NucFrame(nucLabel.value, frame, deadPixels))
    currentNucLabel.value = nucLabel.value
    nucLabel.value = ""
  }
}
