package com.mikrotasarim.api.device

import com.opalkelly.frontpanel._

object OpalKellyInterface {
  val panel = new okCFrontPanel()
}

class DeviceNotFoundException extends Exception("No Opal Kelly device is connected or the connection is held by another process.")

class OpalKellyInterface(bitFileName: String) extends DeviceInterface {

  System.loadLibrary("okjFrontPanel")

  val panel = new okCFrontPanel()

  if (okCFrontPanel.ErrorCode.NoError != OpalKellyInterface.panel.OpenBySerial("")) {
    throw new DeviceNotFoundException
  }

  OpalKellyInterface.panel.LoadDefaultPLLConfiguration()

  if (okCFrontPanel.ErrorCode.NoError != OpalKellyInterface.panel.ConfigureFPGA(bitFileName)) {
    //    OpalKellyInterface.panel.delete()
    throw new Exception("FPGA configuration failed.\n")
  }

  override def setWireInValue(wireNumber: Int, value: Long): Unit = OpalKellyInterface.panel.SetWireInValue(wireNumber, value)

  override def setWireInValue(wireNumber: Int, value: Long, mask: Long): Unit = OpalKellyInterface.panel.SetWireInValue(wireNumber, value, mask)

  override def activateTriggerIn(address: Int, bit: Int): Unit = OpalKellyInterface.panel.ActivateTriggerIn(address, bit)

  // TODO: Test what values work as size parameter.
  override def writeToPipeIn(address: Int, size: Int, data: Array[Byte]): Unit = OpalKellyInterface.panel.WriteToPipeIn(address, 512, data)

  override def readFromPipeOut(address: Int, size: Int, data: Array[Byte]): Unit = {
    OpalKellyInterface.panel.ReadFromPipeOut(address, size, data)
  }

  override def writeToBlockPipeIn(address: Int, blockSize: Int, size: Int, data: Array[Byte]): Unit =
    OpalKellyInterface.panel.WriteToBlockPipeIn(address, blockSize, size, data)

  override def readFromBlockPipeOut(address: Int, size: Int, data: Array[Byte]): Unit = {
    OpalKellyInterface.panel.ReadFromBlockPipeOut(address, 64, size, data)
  }

  override def updateWireIns(): Unit = OpalKellyInterface.panel.UpdateWireIns()

  def isFrontPanel3Supported: Boolean = OpalKellyInterface.panel.IsFrontPanel3Supported()

  def isFrontPanelEnabled: Boolean = OpalKellyInterface.panel.IsFrontPanelEnabled()

  def disconnect(): Unit = {
    //panel.delete()
  }

  override def getWireOutValue(address: Int): Long = OpalKellyInterface.panel.GetWireOutValue(address)

  override def updateWireOuts(): Unit = OpalKellyInterface.panel.UpdateWireOuts()
}
