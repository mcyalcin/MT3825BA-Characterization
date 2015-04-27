package com.mikrotasarim.api.command

import com.mikrotasarim.api.command.ApiConstants.{NucMode, MirrorMode, TriggerMode}
import com.mikrotasarim.api.device.{OpalKellyInterface, MockDeviceInterface}

import org.scalatest.{Matchers, FlatSpec}

class DeviceControllerSpec extends FlatSpec with Matchers {

  "A device controller" should "execute getId command" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.getId
    val expectedOutput = "Wire 1 set to value 192\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\nWire 32 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "execute set system reset command" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setReset()
    val expectedOutput = "Wire 1 set to value 193\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "execute clear system reset command" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.clearReset()
    val expectedOutput = "Wire 1 set to value 194\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set trigger mode" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setTriggerMode(TriggerMode.Master)
    val expectedOutput = "Wire 1 set to value 195\nWire 3 set to value 0\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set window origin" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setWindowOrigin(10, 10)
    val expectedOutput = "Wire 1 set to value 196\nWire 3 set to value 655370\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set window size" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setWindowSize(10, 10)
    val expectedOutput = "Wire 1 set to value 197\nWire 3 set to value 655370\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set pixel gain" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setPixelGain(10)
    val expectedOutput = "Wire 1 set to value 198\nWire 3 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set mirror mode" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setMirrorMode(MirrorMode.NoMirroring)
    val expectedOutput = "Wire 1 set to value 199\nWire 3 set to value 0\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set output mode" in {
    // TODO: Pending specification
  }

  it should "set integration time" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setIntegrationTime(10)
    val expectedOutput = "Wire 1 set to value 201\nWire 3 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\nWire 1 set to value 203\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set frame time" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setFrameTime(10)
    val expectedOutput = "Wire 1 set to value 202\nWire 3 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "write to ROIC memory" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.writeToRoicMemory(10, 10)
    val expectedOutput = "Wire 1 set to value 207\nWire 3 set to value 10\nWire 2 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\nWire 1 set to value 203\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set reference voltage" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setReferenceVoltage(10)
    val expectedOutput = "Wire 1 set to value 204\nWire 3 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "initialize ROIC" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.initializeRoic()
    val expectedOutput = "Wire 1 set to value 205\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "read from ROIC memory" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.readFromRoicMemory(10)
    val expectedOutput = "Wire 1 set to value 206\nWire 2 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\nWire 32 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set global reference bias" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setGlobalReferenceBias(10)
    val expectedOutput = "Wire 1 set to value 177\nWire 3 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set pixel bias range" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setPixelBiasRange(10)
    val expectedOutput = "Wire 1 set to value 178\nWire 3 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set active flash partition" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setActiveFlashPartition(10)
    val expectedOutput = "Wire 1 set to value 179\nWire 2 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "get active flash partition" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.getActiveFlashPartition
    val expectedOutput = "Wire 1 set to value 180\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\nWire 32 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "read from flash" in {
    // TODO: Pending specification
  }

  it should "write to flash" in {
    // TODO: Pending specification
  }

  it should "erase flash partition" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.eraseActiveFlashPartition()
    val expectedOutput = "Wire 1 set to value 183\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "update reference data" in {
    // TODO: Pending specification
  }

  it should "set NUC mode" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setNucMode(NucMode.Disabled)
    val expectedOutput = "Wire 1 set to value 186\nWire 3 set to value 0\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set NUC mode with fixed data" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setNucMode(NucMode.Fixed, 10)
    val expectedOutput = "Wire 1 set to value 186\nWire 3 set to value 2562\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "set pixel midpoint" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.setPixelMidpoint(10)
    val expectedOutput = "Wire 1 set to value 187\nWire 3 set to value 10\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "disable imaging mode" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.disableImagingMode()
    val expectedOutput = "Wire 1 set to value 224\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }

  it should "enable imaging mode" in {
    val outputBuffer = new StringBuilder
    val device = new MockDeviceInterface(outputBuffer)
    val deviceController = new DeviceController(device)
    deviceController.enableImagingMode()
    val expectedOutput = "Wire 1 set to value 225\nWire Ins Updated\nTrigger 64 set to value 0\nWire Outs Updated\nWire 33 read\n"
    outputBuffer.toString() should be(expectedOutput)
  }
}
