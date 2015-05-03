package com.mikrotasarim.api.device

class MockDeviceInterface(outputBuffer: StringBuilder) extends DeviceInterface {
  override def setWireInValue(wireNumber: Int, value: Long) {
    outputBuffer.append("Wire " + wireNumber + " set to value " + value + "\n")
  }

  override def activateTriggerIn(address: Int, bit: Int) {
    outputBuffer.append("Trigger " + address + " set to value " + bit + "\n")
  }

  override def updateWireIns() {
    outputBuffer.append("Wire Ins Updated\n")
  }

  override def writeToPipeIn(address: Int, size: Int, data: Array[Byte]) {
    outputBuffer.append("A data array of size " + data.length + " claimed to be of size " + size + " written to pipe " + address + "\n")
  }

  override def writeToBlockPipeIn(address: Int, blockSize: Int, size: Int, data: Array[Byte]): Unit = ???

  override def disconnect() {
    outputBuffer.append("Device disconnected.")
  }

  override def setWireInValue(wireNumber: Int, value: Long, mask: Long): Unit =
    outputBuffer.append("Wire " + wireNumber + " set to value " + value + " with mask " + mask + "\n")

  override def getWireOutValue(address: Int): Long = {
    outputBuffer.append("Wire " + address + " read\n")
    0
  }

  override def updateWireOuts(): Unit = {
    outputBuffer.append("Wire Outs Updated\n")
  }

  override def readFromPipeOut(address: Int, size: Int, data: Array[Byte]): Unit = {
    outputBuffer.append(size + " bytes read from " + address)
  }

  override def readFromBlockPipeOut(address: Int, size: Int, data: Array[Byte]): Unit = ???
}
