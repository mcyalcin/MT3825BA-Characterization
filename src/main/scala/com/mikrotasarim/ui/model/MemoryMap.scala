package com.mikrotasarim.ui.model

import scalafx.beans.property.StringProperty

import com.mikrotasarim.ui.controller.FpgaController.deviceController

object MemoryMap {

  val minMemoryIndex = 4
  val maxMemoryIndex = 95

  val memoryLocations = for (i <- minMemoryIndex to maxMemoryIndex) yield new MemoryLocation(i)

  def readRoicMemory(): Unit = {
    for (memoryLocation <- memoryLocations) {
      memoryLocation.read()
    }
  }

  class MemoryLocation(val address: Int) {
    val text = StringProperty("0000000000000000")

    def read(): Unit = {
      val value = deviceController.readFromRoicMemory(address)
      text.value = String.format("%16s", value.toBinaryString).replace(' ', '0')
    }

    def commit(): Unit = {
      val value = java.lang.Long.parseLong(text.value, 2)
      deviceController.writeToRoicMemory(address, value)
      read()
    }
  }

}
