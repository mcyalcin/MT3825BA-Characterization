package com.mikrotasarim.api.device

trait DeviceInterface {
  def updateWireOuts()

  def getWireOutValue(address: Int): Long

  def setWireInValue(wireNumber: Int, value: Long)

  def setWireInValue(wireNumber: Int, value: Long, mask: Long)

  def activateTriggerIn(address: Int, bit: Int)

  def writeToPipeIn(address: Int, size: Int, data: Array[Byte])

  def readFromPipeOut(address: Int, size: Int, data: Array[Byte])

  def readFromBlockPipeOut(address: Int, size: Int, data: Array[Byte])

  def writeToBlockPipeIn(address: Int, blockSize: Int, size: Int, data: Array[Byte])

  def updateWireIns()

  def disconnect()
}
