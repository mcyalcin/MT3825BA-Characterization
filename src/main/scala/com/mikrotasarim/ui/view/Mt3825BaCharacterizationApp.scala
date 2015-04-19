package com.mikrotasarim.ui.view

import com.mikrotasarim.ui.model.MemoryMap

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.geometry.Insets
import scalafx.scene.layout._
import scalafx.scene.{Node, Scene}
import scalafx.scene.control._

import com.mikrotasarim.ui.model.FileControlModel._
import com.mikrotasarim.ui.controller.FpgaController._

object Mt3825BaCharacterizationApp extends JFXApp {

  def Reset(): Unit = {
    stage.scene = new Scene(stage.scene.width.intValue(), stage.scene.height.intValue()) {
      root = new BorderPane {
        left = GeneralControls
        center = ControlTabs
        right = ImageDisplay
      }
    }
  }

  stage = new PrimaryStage {
    title = "Mikro-Tasarim MT3825BA Characterization App"
  }

  stage.scene = new Scene {
    root = new BorderPane {
      left = GeneralControls
      center = ControlTabs
      right = ImageDisplay
    }
  }

  stage.setMaximized(true)

  def ControlTabs: Node = new TabPane {
    tabs = List(
      new Tab {
        text = "Image"
        closable = false
        content = ImageControlPanel
      },
      new Tab {
        text = "Calibration"
        closable = false
        content = CalibrationControlPanel
      },
      new Tab {
        text = "Measurement"
        closable = false
        content = MeasurementControlPanel
      }
    )
  }

  def ImageControlPanel: Node = new ScrollPane {
    content = new VBox {
      padding = Insets(10)
      spacing = 20

      content = List(
        ImageSaveControls
      )
    }
  }

  def ImageSaveControls: Node = new VBox {
    spacing = 10

    content = List(
      new Label("Save Images"),
      ImagePrefixSelector,
      ImageNumberSelector,
      ImageSaveButton
    )
  }

  def ImagePrefixSelector: Node = new TextField {
    prefColumnCount = 15
    promptText = "File prefix"
    text <==> filePrefix
  }

  def ImageNumberSelector: Node = new TextField {
    prefColumnCount = 15
    promptText = "Sample count"
    text <==> sampleCount
  }

  def ImageSaveButton: Node = new Button("Save") {
    onAction = handle {
      SaveImages()
    }
  }

  def CalibrationControlPanel: Node = new VBox

  def MeasurementControlPanel: Node = new VBox

  def ImageDisplay: Node = new VBox

  def GeneralControls: Node = new VBox {
    padding = Insets(10)
    spacing = 20
    content = List(
      ConnectButton,
      DisconnectButton,
      ResetButton,
      MemoryMapButton
    )
  }

  def ConnectButton: Node = new Button("Connect") {
    onAction = handle {
      ConnectToFpga()
    }
  }

  def DisconnectButton: Node = new Button("Disconnect") {
    onAction = handle {
      DisconnectFromFpga()
    }
  }

  def ResetButton: Node = new Button("Reset") {
    onAction = handle {
      ResetChip()
    }
  }

  def MemoryMapButton: Node = new Button("Memory Map") {
    onAction = () => {
      MemoryMap.ReadRoicMemory()
      MemoryMapStage.show()
    }
  }
}
