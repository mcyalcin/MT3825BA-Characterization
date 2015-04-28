package com.mikrotasarim.ui.view

import com.mikrotasarim.ui.model.MemoryMap

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.beans.property.StringProperty
import scalafx.geometry.Insets
import scalafx.scene.layout._
import scalafx.scene.{Node, Scene}
import scalafx.scene.control._

import com.mikrotasarim.ui.model.FileControlModel._
import com.mikrotasarim.ui.controller.FpgaController._

object Mt3825BaCharacterizationApp extends JFXApp {

  stage = new PrimaryStage {
    title = "Mikro-Tasarim MT3825BA Characterization App"
  }

  stage.scene = new Scene {
    root = new BorderPane {
      left = generalControls
      center = controlTabs
    }
  }

  stage.setMaximized(true)

  def controlTabs: Node = new TabPane {
    disable <== !deviceConnected
    tabs = List(
      new Tab {
        text = "Image"
        closable = false
        content = imageControlPanel
      },
      new Tab {
        text = "Calibration"
        closable = false
        content = calibrationControlPanel
      },
      new Tab {
        text = "Measurement"
        closable = false
        content = measurementControlPanel
      }
    )
  }

  def imageControlPanel: Node = new ScrollPane {
    content = new VBox {
      padding = Insets(10)
      spacing = 20

      content = List(
        windowingControls,
        new Separator,
        imageSaveControls,
        new Separator,
        imageOpenButton
      )
    }
  }

  def windowingControls: Node = new VBox {
    spacing = 10

    content = List(
      new HBox {
        spacing = 10
        content = List(xOriginBox, xSizeBox)
      },
      new HBox {
        spacing = 10
        content = List(yOriginBox, ySizeBox)
      },
      new HBox {
        spacing = 10
        content = List(
          new Button("Apply"),
          new Button("Default") {
            onAction = handle {
              resetWindowSize()
            }
          }
        )
      }

    )
  }

  def box(label: String, property: StringProperty): Node = new HBox {
    spacing = 10
    content = List(
      new Label(label),
      new TextField {
        prefColumnCount = 5
        text <==> property
      }
    )
  }

  def xOriginBox: Node = box("x Origin", xOrigin)

  def xSizeBox: Node = box("x Size", xSize)

  def yOriginBox: Node = box("y Origin", yOrigin)

  def ySizeBox: Node = box("y Size", ySize)

  def imageSaveControls: Node = new VBox {
    spacing = 10

    content = List(
      new Label("Save Images"),
      imagePrefixSelector,
      imageNumberSelector,
      imageSaveButton
    )
  }

  def imageOpenButton: Node = new Button("Open") {
    onAction = handle {
      openImage()
    }
  }

  def imagePrefixSelector: Node = new TextField {
    prefColumnCount = 15
    promptText = "File prefix"
    text <==> filePrefix
  }

  def imageNumberSelector: Node = new TextField {
    prefColumnCount = 15
    promptText = "Sample count"
    text <==> sampleCount
  }

  def imageSaveButton: Node = new Button("Save") {
    onAction = handle {
      saveImages()
    }
  }

  def calibrationControlPanel: Node = new VBox {
    disable = true
  }

  def measurementControlPanel: Node = new VBox {
    disable = true
  }

  def generalControls: Node = new VBox {
    padding = Insets(10)
    spacing = 20
    content = List(
      connectButton,
      disconnectButton,
      memoryMapButton,
      selfTestModeSelector
    )
  }

  def selfTestModeSelector: Node = new HBox {
    spacing = 10
    content = List(
      new Label("Self test"),
      new CheckBox {
        disable <== deviceConnected
        selected <==> isSelfTest
      }
    )
  }

  def connectButton: Node = new Button("Connect") {
    disable <== deviceConnected
    onAction = handle {
      connectToFpga()
    }
  }

  def disconnectButton: Node = new Button("Disconnect") {
    disable <== !deviceConnected
    onAction = handle {
      disconnectFromFpga()
    }
  }

  def memoryMapButton: Node = new Button("Memory Map") {
    disable <== !deviceConnected
    onAction = () => {
      MemoryMap.readRoicMemory()
      MemoryMapStage.show()
    }
  }
}
