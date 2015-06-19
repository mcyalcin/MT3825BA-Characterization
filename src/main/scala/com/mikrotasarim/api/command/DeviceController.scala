package com.mikrotasarim.api.command

import com.mikrotasarim.api.command.ApiConstants.ResistanceMeasurementMode.ResistanceMeasurementMode
import com.mikrotasarim.api.device.DeviceInterface

import spire.implicits._

class ImagingInProgressException extends Exception("Imaging in progress.")

class DeviceController(device: DeviceInterface) {

  import ApiConstants._
  import ApiConstants.TriggerMode._
  import ApiConstants.MirrorMode._
  import ApiConstants.NucMode._

  def takeFpgaOffReset(): Unit = {
    takeOffReset(clockReset)
    takeOffReset(systemReset)
  }

  def takeOffReset(reset: Int): Unit = {
    device.setWireInValue(resetWire, 2 pow reset, 2 pow reset)
    device.updateWireIns()
  }

  def putFpgaOnReset(): Unit = {
    putOnReset(systemReset)
    putOnReset(clockReset)
  }

  def putOnReset(reset: Int): Unit = {
    device.setWireInValue(resetWire, 0, 2 pow reset)
    device.updateWireIns()
  }

  def waitForDeviceReady(): Unit = {
    do {
      device.updateWireOuts()
    } while (device.getWireOutValue(statusWire) != 0)
  }

  private def setWiresAndTrigger(wires: Map[Int, Long]): Unit = {
    waitForDeviceReady()
    for (wire <- wires.keys) {
      device.setWireInValue(wire, wires(wire))
    }
    device.updateWireIns()
    device.activateTriggerIn(triggerWire, 0)
    device.updateWireOuts()
    val errorCode = device.getWireOutValue(errorWire)
    checkErrorCode(errorCode)
  }

  private def checkErrorCode(errorCode: Long): Unit = {

    errorCode % 65536 match {
      case 0 =>
      case 0x0ff => throw new Exception("Invalid command.")
      case 0xbe0 => throw new Exception("Integration time too low.")
      case 0xbe1 => throw new Exception("Frame time too low.")
      case 0xbe2 => throw new ImagingInProgressException
      case 0xbe3 => throw new Exception("Invalid NUC mode.")
      case 0xbe4 => throw new Exception("Invalid trigger mode.")
      case 0xbe5 => throw new Exception("SPI busy.")
      case 0xbf0 => throw new Exception("Flash busy.")
      case 0xbf1 => throw new Exception("Flash not ready.")
      case 0xbf2 => throw new Exception("Invalid flash partition.")
      case 0xbf3 => throw new Exception("Imaging not enabled.")
      case 0xbf4 => throw new Exception("Integration time too high.")
      case 0xeeee => throw new Exception("Device on reset.")
      case default => throw new Exception("Unexpected error code: " + default)
    }
  }

  def getId: Long = {
    setWiresAndTrigger(Map(
      commandWire -> getIdOpCode
    ))
    device.getWireOutValue(readWire)
  }

  def setReset() = {
    setWiresAndTrigger(Map(
      commandWire -> setRsOpCode
    ))
  }

  def clearReset() = {
    setWiresAndTrigger(Map(
      commandWire -> clrRsOpCode
    ))
  }

  def setTriggerMode(triggerMode: TriggerMode) = {
    setWiresAndTrigger(Map(
      commandWire -> sTrMdOpCode,
      dataWire -> triggerMode.id
    ))
  }

  def setWindowOrigin(xOrigin: Long, yOrigin: Long) = {
    setWiresAndTrigger(Map(
      commandWire -> sWOrgOpCode,
      dataWire -> (xOrigin * 65536 + yOrigin)
    ))
  }

  def setWindowSize(xSize: Long, ySize: Long) = {
    setWiresAndTrigger(Map(
      commandWire -> sWSizOpCode,
      dataWire -> (xSize * 65536 + ySize)
    ))
  }

  def setPixelGain(pixelGainDenominator: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sPixGOpCode,
      dataWire -> pixelGainDenominator
    ))
    updateRoicMemory()
  }

  def setMirrorMode(mode: MirrorMode): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sMirMOpCode,
      dataWire -> mode.id
    ))
  }

  def setIntegrationTime(clockCycles: Long): Unit = {
    // Operating clock speed is 3 MHz.
    setWiresAndTrigger(Map(
      commandWire -> stIntOpCode,
      dataWire -> clockCycles
    ))
    updateRoicMemory()
  }

  def setFrameTime(time: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> stFrmOpCode,
      dataWire -> time
    ))
  }

  def updateRoicMemory(): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> uRoicOpCode
    ))
  }

  def writeToRoicMemoryBuffer(address: Int, value: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> wRoicOpCode,
      dataWire -> value,
      addressWire -> address
    ))
  }

  def writeToRoicMemory(address: Int, value: Long): Unit = {
    writeToRoicMemoryBuffer(address, value)
    updateRoicMemory()
  }

  // Input is 0-3V with 12 bit resolution.
  // TODO: Decide where the mapping and conversion is to be done, then update if needed.
  def setReferenceVoltage(value: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sVRefOpCode,
      dataWire -> value
    ))
    updateRoicMemory()
  }

  def initializeRoic(): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> initROpCode
    ))
  }

  def readFromRoicMemory(address: Long): Long = {
    setWiresAndTrigger(Map(
      commandWire -> rRoicOpCode,
      addressWire -> address
    ))
    device.getWireOutValue(readWire)
  }

  // Input is 0-3V with 12 bit resolution.
  // TODO: Decide where the mapping and conversion is to be done, then update if needed.
  def setGlobalReferenceBias(value: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sGRfBOpCode,
      dataWire -> value
    ))
    updateRoicMemory()
  }

  // Input is 0-1.5V with 12 bit resolution.
  // TODO: Decide where the mapping and conversion is to be done, then update if needed.
  def setPixelBiasRange(value: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sPxBROpCode,
      dataWire -> value
    ))
    updateRoicMemory()
  }

  def setActiveFlashPartition(address: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sFPrtOpCode,
      addressWire -> address
    ))
  }

  def getActiveFlashPartition: Long = {
    setWiresAndTrigger(Map(
      commandWire -> gFPrtOpCode
    ))
    device.getWireOutValue(readWire)
  }

  def readFromFlashMemory(index: Int): Array[Byte] = {
    resetFlashOutFifo()
    setWiresAndTrigger(Map(
      commandWire -> rFMemOpCode,
      addressWire -> index
    ))
    val line = Array.ofDim[Byte](lineSize)
    device.readFromPipeOut(flashFifoOutPipe, line.length, line)
    line
  }

  def readFrameFromFlashMemory(): Array[Array[Byte]] = {
    val frame = Array.ofDim[Array[Byte]](numRows)
    for (rowIndex <- 0 until numRows) {
      frame(rowIndex) = readFromFlashMemory(rowIndex)
    }
    frame
  }

  private def resetFlashOutFifo(): Unit = {
    waitForDeviceReady()
    device.setWireInValue(resetWire, 0, 2 pow flashFifoOutReset)
    device.updateWireIns()
    device.setWireInValue(resetWire, 2 pow flashFifoOutReset, 2 pow flashFifoOutReset)
    device.updateWireIns()
  }

  private def resetFlashInFifo(): Unit = {
    waitForDeviceReady()
    device.setWireInValue(resetWire, 0, 2 pow flashFifoInReset)
    device.updateWireIns()
    device.setWireInValue(resetWire, 2 pow flashFifoInReset, 2 pow flashFifoInReset)
    device.updateWireIns()
  }

  def writeToFlashMemory(line: Array[Byte], index: Int) = {
    resetFlashInFifo()
    device.writeToPipeIn(flashFifoInPipe, line.length, line)
    setWiresAndTrigger(Map(
      commandWire -> wFMemOpCode,
      addressWire -> index
    ))
  }

  def writeFrameToFlashMemory(frame: Array[Array[Byte]]): Unit = {
    for (line <- frame.zipWithIndex) {
      writeToFlashMemory(line._1, line._2)
    }
  }

  def eraseActiveFlashPartition(): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> eFPrtOpCode
    ))
  }

  def updateReferenceDataOnFlashMemory(referenceData: Array[Byte]): Unit = {
    resetFlashInFifo()
    device.writeToPipeIn(flashFifoInPipe, referenceData.length, referenceData)
    setWiresAndTrigger(Map(
      commandWire -> upRefOpCode
    ))
  }

  def sendReferenceDataToRoic(): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sDReROpCode
    ))
  }

  def updateReferenceData(referenceData: Array[Byte]): Unit = {
    updateReferenceDataOnFlashMemory(referenceData)
    sendReferenceDataToRoic()
  }

  def setNucMode(mode: NucMode): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sTNucOpCode,
      dataWire -> mode.id
    ))
  }

  def setNucMode(mode: NucMode, fixedData: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sTNucOpCode,
      dataWire -> (mode.id + 256 * fixedData + 65536 * fixedData)
    ))
  }

  def setNucMode(mode: NucMode, topData: Long, botData: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sTNucOpCode,
      dataWire -> (mode.id + 256 * topData + 65536 * botData)
    ))
  }

  def setPixelMidpoint(value: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sPxMpOpCode,
      dataWire -> value
    ))
    updateRoicMemory()
  }

  def disableImagingMode(): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> disImOpCode
    ))
  }

  def enableImagingMode(): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> enbImOpCode
    ))
  }

  def getFrameData(frameSize: Int): Array[Byte] = {
    setWiresAndTrigger(Map(
      commandWire -> sFsynOpCode
    ))
    val rawFrame = Array.ofDim[Byte](frameSize)
    do {
      device.updateWireOuts()
    } while (device.getWireOutValue(readyWire) != 0)
    device.readFromPipeOut(imageOutPipe, frameSize, rawFrame)
    rawFrame
  }

  def getFrame: Array[Byte] = {
    val fullFrame = getFullFrame

    var clippedFrame = fullFrame.drop(392 * 2)

    for (i <- 0 until 288) {
      clippedFrame = clippedFrame.take(i * 384 * 2 + 384 * 2) ++ clippedFrame.drop(i * 384 * 2 + 392 * 2)
    }

    clippedFrame
  }

  def getFullFrame: Array[Byte] = {
    setWiresAndTrigger(Map(
      commandWire -> sFsynOpCode
    ))
    val frameSize = 392 * 289 * 2
    val rawFrame = Array.ofDim[Byte](frameSize)
    do {
      device.updateWireOuts()
      // TODO: Add actors/multithreading here to avoid busy waiting
    } while (device.getWireOutValue(readyWire) != 0)
    device.readFromPipeOut(imageOutPipe, frameSize, rawFrame)
    rawFrame
  }

  def setAdcDelay(delay: Long): Unit = {
    device.setWireInValue(delayWire, delay, 0x0000000F)
  }

  def setSamplingDelay(delay: Long): Unit = {
    device.setWireInValue(delayWire, delay * 16, 0x000001F0)
  }

  def setTestMode(mode: Long): Unit = {
    device.setWireInValue(delayWire, mode * 512, 0x00000200)
  }

  def setResistanceMeasurementMode(mode: ResistanceMeasurementMode): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sResMOpCode,
      dataWire -> mode.id
    ))
  }
}

object ApiConstants {

  val resetWire = 0x00
  val commandWire = 0x01
  val addressWire = 0x02
  val dataWire = 0x03
  val delayWire = 0x04

  val readWire = 0x20
  val errorWire = 0x21
  val statusWire = 0x22
  val readyWire = 0x23

  val triggerWire = 0x40

  val flashFifoInPipe = 0x80

  val flashFifoOutPipe = 0xa0
  val nucOutPipe = 0xa1
  val imageOutPipe = 0xa2

  val systemReset = 0
  val clockReset = 1
  val flashFifoInReset = 2
  val flashFifoOutReset = 3
  val nucFifoOutReset = 4

  val statusFlashReadyBit = 0

  val lineSize = 384
  val numRows = 289

  val getIdOpCode = 0xc0
  val setRsOpCode = 0xc1
  val clrRsOpCode = 0xc2
  val sTrMdOpCode = 0xc3
  val sWOrgOpCode = 0xc4
  val sWSizOpCode = 0xc5
  val sPixGOpCode = 0xc6
  val sMirMOpCode = 0xc7
  val sOutMOpCode = 0xc8
  val stIntOpCode = 0xc9
  val stFrmOpCode = 0xca
  val uRoicOpCode = 0xcb
  val sVRefOpCode = 0xcc
  val initROpCode = 0xcd
  val rRoicOpCode = 0xce
  val wRoicOpCode = 0xcf
  val sGRfBOpCode = 0xb1
  val sPxBROpCode = 0xb2
  val sFPrtOpCode = 0xb3
  val gFPrtOpCode = 0xb4
  val rFMemOpCode = 0xb5
  val wFMemOpCode = 0xb6
  val eFPrtOpCode = 0xb7
  val upRefOpCode = 0xb8
  val sDReROpCode = 0xb9
  val sTNucOpCode = 0xba
  val sPxMpOpCode = 0xbb
  val sResMOpCode = 0xbe
  val disImOpCode = 0xe0
  val enbImOpCode = 0xe1
  val sFsynOpCode = 0xe2

  object TriggerMode extends Enumeration {
    type TriggerMode = Value
    val Master, Slave_Hardware, Slave_Software = Value
  }

  object MirrorMode extends Enumeration {
    type MirrorMode = Value
    val NoMirroring, X_Mirror, Y_Mirror, XY_Mirror = Value
  }

  object ResistanceMeasurementMode extends Enumeration {
    type ResistanceMeasurementMode = Value
    val Detector, Reference = Value
  }

  object NucMode extends Enumeration {
    type NucMode = Value
    val Disabled, Enabled, Fixed = Value
  }

}