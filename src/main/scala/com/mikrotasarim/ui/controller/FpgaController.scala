package com.mikrotasarim.ui.controller

import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.api.device.ConsoleMockDeviceInterface

object FpgaController {

  var DeviceController = new DeviceController(new ConsoleMockDeviceInterface)

  def ConnectToFpga(): Unit = {

  }

  def DisconnectFromFpga(): Unit = {

  }

  def ResetChip(): Unit = {

  }
}
