package com.mikrotasarim.api.device

class ConsoleMockDeviceInterface extends DeviceInterface {
  override def setWireInValue(wireNumber: Int, value: Long) {
    println("Wire " + wireNumber + " set to value " + value)
  }

  override def activateTriggerIn(address: Int, bit: Int) {
    println("Trigger " + address + " set to value " + bit)
  }

  override def updateWireIns() {
    println("Wire Ins Updated")
  }

  override def writeToPipeIn(address: Int, size: Int, data: Array[Byte]) {
    println("A data array of size " + data.length + " claimed to be of size " + size + " written to pipe " + address)
  }

  override def writeToBlockPipeIn(address: Int, blockSize: Int, size: Int, data: Array[Byte]): Unit = ???

  override def disconnect() {
    println("Device disconnected.")
  }

  override def setWireInValue(wireNumber: Int, value: Long, mask: Long): Unit =
    println("Wire " + wireNumber + " set to value " + value + " with mask " + mask)

  override def getWireOutValue(address: Int): Long = {
    println(address + " read")
    0
  }

  override def updateWireOuts(): Unit = println("Wire Outs Updated")

  override def readFromPipeOut(address: Int, size: Int, data: Array[Byte]): Unit = {
    println(size + " bytes read from " + address)
  }

  override def readFromBlockPipeOut(address: Int, size: Int, data: Array[Byte]): Unit = {}
}

