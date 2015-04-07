package com.mikrotasarim.api.command

import com.mikrotasarim.api.device.DeviceInterface

class DeviceController(device: DeviceInterface) {

  import ApiConstants._
  import ApiConstants.TriggerMode._
  import ApiConstants.MirrorMode._
  import ApiConstants.NucMode._

  private def setWiresAndTrigger(wires: Map[Int, Long]): Unit = {
    for (wire <- wires.keys) {
      device.setWireInValue(wire, wires(wire))
    }
    device.updateWireIns()
    device.activateTriggerIn(triggerWire, 0)
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
  }

  def setMirrorMode(mode: MirrorMode): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sMirMOpCode,
      dataWire -> mode.id
    ))
  }

  // TODO: Pending specification
  def setOutputMode() = ???

  def setIntegrationTime(time: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sIntTOpCode,
      dataWire -> time
    ))
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
  }

  // Input is 0-1.5V with 12 bit resolution.
  // TODO: Decide where the mapping and conversion is to be done, then update if needed.
  def setPixelBiasRange(value: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sPxBROpCode,
      dataWire -> value
    ))
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

  // TODO: Decide how flash memory data is to be stored. Check how in pipes work. Implement these accordingly.
  def readFromFlashMemory() = ???
  def writeToFlashMemory() = ???

  def eraseActiveFlashPartition(): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> eFPrtOpCode
    ))
  }

  def updateReferenceDataOnFlashMemory() = ???

  def sendReferenceDataToRoic(): Unit = {
    setWiresAndTrigger(Map(
     commandWire -> sDReROpCode
    ))
  }

  def updateReferenceData(): Unit = {
    updateReferenceDataOnFlashMemory()
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
      dataWire -> (mode.id + 256 * fixedData)
    ))
  }

  def setPixelMidpoint(value: Long): Unit = {
    setWiresAndTrigger(Map(
      commandWire -> sPxMpOpCode,
      dataWire -> value
    ))
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
}

object ApiConstants {

  val commandWire = 0x01
  val addressWire = 0x02
  val dataWire = 0x03

  val readWire = 0x20

  val triggerWire = 0x40

  val getIdOpCode = 0xc0
  val setRsOpCode = 0xc1
  val clrRsOpCode = 0xc2
  val sTrMdOpCode = 0xc3
  val sWOrgOpCode = 0xc4
  val sWSizOpCode = 0xc5
  val sPixGOpCode = 0xc6
  val sMirMOpCode = 0xc7
  val sOutMOpCode = 0xc8
  val sIntTOpCode = 0xc9
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
  val disImOpCode = 0xe0
  val enbImOpCode = 0xe1

  object TriggerMode extends Enumeration {
    type TriggerMode = Value
    val Master, Slave_Hardware, Slave_Software = Value
  }

  object MirrorMode extends Enumeration {
    type MirrorMode = Value
    val NoMirroring, X_Mirror, Y_Mirror, XY_Mirror = Value
  }

  // TODO: Put in meaningful names for this enumeration
  object NucMode extends Enumeration {
    type NucMode = Value
    val mode_00, mode_01, mode_10 = Value
  }
}