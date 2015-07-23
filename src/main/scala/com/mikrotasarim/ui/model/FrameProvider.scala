package com.mikrotasarim.ui.model

import com.mikrotasarim.api.command.DeviceController
import com.mikrotasarim.ui.controller.{MeasurementController, FpgaController}

abstract class FrameProvider(val dc: DeviceController, initialXSize: Int, initialYSize: Int) {
  val deadLines: Int
  val depth: Int = 16383
  val tempPixels: Int = 8

  var xSize: Int = initialXSize
  var ySize: Int = initialYSize

  def xS: Int = xSize + tempPixels
  def yS: Int = ySize + deadLines

  def getFrameData: Array[Byte] = {
    dc.getFrameDataFromBtPipe(xS * yS * 2)
  }

  def getClippedFrame: IndexedSeq[Int] = {
    combineBytes(getClippedFrameData)
  }

  def getClippedFrameData: IndexedSeq[Byte] = {
    val frameData = getFrameData
    val clippedFrameData = frameData.drop(deadLines * xSize * 2).zipWithIndex.filter(_._2 % (xS * 2) < (xSize * 2)).map(_._1)
    clippedFrameData
  }

  def getFrame: Frame = {
    val frameData = getFrameData
    val efficientClippedFrameData = Array.ofDim[Byte](xSize * ySize * 2)
    var nextValid = deadLines * xSize * 2
    for (i <- efficientClippedFrameData.indices) {
      if (nextValid % (xS * 2) == (xSize * 2)) nextValid += tempPixels * 2
      efficientClippedFrameData(i) = frameData(nextValid)
      nextValid += 1
    }
    val combinedFrameData = correctImage(combineBytes(efficientClippedFrameData))
    val frame = Frame.createFrom14Bit(xSize, ySize, combinedFrameData)
    frame
  }

  def combineBytes(raw: IndexedSeq[Byte]): IndexedSeq[Int] = {
    def unsigned(b: Byte): Int = {
      (b + 256) % 256
    }
    (for (i <- 0 until (raw.length / 2)) yield unsigned(raw(2*i)) + unsigned(raw(2*i+1))*256).toArray
  }


  def correctImage(frameData: IndexedSeq[Int]): IndexedSeq[Int] = {
    def onePointCorrect(img: IndexedSeq[Int]): IndexedSeq[Int] =
      (for (i <- 0 until xSize * ySize) yield Seq(0, img(i) - MeasurementController.measurement.dark(i)).max).toArray

    def twoPointCorrect(img: IndexedSeq[Int]): IndexedSeq[Int] = {
      (for (i <- 0 until xSize * ySize) yield
      (MeasurementController.measurement.slope(i) *
        Seq(0, img(i) - MeasurementController.measurement.dark(i)).max).toInt).toArray
    }

    if (FpgaController.correctionEnabled.value)
      if (FpgaController.onePointCorrection.value) onePointCorrect(frameData)
      else twoPointCorrect(frameData)
    else frameData
  }

  def getRawFrame: Frame = {
    val frameData = getFrameData
    val frame = Frame.createFromRaw(xS, yS, frameData, depth)
    frame
  }

  def getFullFrame: Frame = {
    val frameData = getFrameData
    val clippedFrameData = frameData.drop(deadLines * xSize * 2)
    val frame = Frame.createFromRaw(xSize, ySize, clippedFrameData, depth)
    frame
  }

  def getEvenFrame: Frame = {
    val frameData = getFrameData
    val clippedFrameData = frameData.drop(deadLines * xSize * 2).zipWithIndex.filter(_._2 % (xS * 2) < (xSize * 2)).filter(_._2 % 2 == 0).map(_._1)
    val frame = Frame.createFromRaw(xSize / 2, ySize, clippedFrameData, depth)
    frame
  }

  def getTempFrame: Frame = {
    val frameData = getFrameData
    val clippedFrameData = frameData.drop(deadLines * xSize * 2).zipWithIndex.filter(_._2 % (xS * 2) >= (xSize * 2)).map(_._1)
    val frame = Frame.createFromRaw(tempPixels, ySize, clippedFrameData, depth)
    frame
  }

  def getOddFrame: Frame = {
    val frameData = getFrameData
    val clippedFrameData = frameData.drop(deadLines * xSize * 2).zipWithIndex.filter(_._2 % (xS * 2) < (xSize * 2)).filter(_._2 % 2 == 1).map(_._1)
    val frame = Frame.createFromRaw(xSize / 2, ySize, clippedFrameData, depth)
    frame
  }
}

class A0FrameProvider(dc: DeviceController, initialXSize: Int, initialYSize: Int) extends FrameProvider(dc: DeviceController, initialXSize: Int, initialYSize: Int) {
  override val deadLines = 1
}

class A1FrameProvider(dc: DeviceController, initialXSize: Int, initialYSize: Int) extends FrameProvider(dc: DeviceController, initialXSize: Int, initialYSize: Int) {
  override val deadLines = 0
}