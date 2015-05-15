package com.mikrotasarim.ui.view

import com.mikrotasarim.api.device.DeviceNotFoundException
import com.mikrotasarim.ui.controller.CalibrationController._
import com.mikrotasarim.ui.controller.FpgaController._
import com.mikrotasarim.ui.controller.ImageController._
import com.mikrotasarim.ui.controller.{FpgaController, CalibrationController, MeasurementController}
import com.mikrotasarim.ui.model.{Measurement, MemoryMap}
import org.controlsfx.dialog.Dialogs

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.beans.property.{IntegerProperty, StringProperty}
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.scene.{Node, Scene}

object Mt3825BaCharacterizationApp extends JFXApp {

  object MyUncaughtExceptionHandler extends Thread.UncaughtExceptionHandler {
    // TODO: Expand this
    def uncaughtException(thread: Thread, e: Throwable): Unit = e match {
      case e: UnsatisfiedLinkError => Dialogs.create()
        .title("Error")
        .masthead("Unsatisfied Link")
        .message("Opal Kelly driver not in java library path.")
        .showException(e)
      case e: DeviceNotFoundException => Dialogs.create()
        .title("Device Not Found Exception")
        .masthead("Device Not Found")
        .message(e.getMessage)
        .showException(e)
      case e: Exception => Dialogs.create()
        .title("Exception")
        .masthead("Unhandled Exception")
        .message(e.getMessage)
        .showException(e)
      case e: Error => Dialogs.create()
        .title("Error")
        .masthead("Unhandled Error")
        .message(e.getMessage)
        .showException(e)
      case _ => println("???")
    }
  }

  Thread.currentThread().setUncaughtExceptionHandler(MyUncaughtExceptionHandler)

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

  def calibrationControlPanel: Node = new ScrollPane {
    content = new VBox {
      padding = Insets(10)
      spacing = 20
      content = List(
        nucControls,
        new Separator,
        correctionControls,
        new Separator,
        globalReferenceBiasSlider,
//        pixelBiasSlider,
        integrationTimeSlider,
        adcDelaySlider
      )
    }
  }

  def labeledSnappingSliderGroup(label: String, model: IntegerProperty, mini: Int, maxi: Int, increment: Int, unitLabel: String, apply: () => Unit, reset: () => Unit): Node =
    new HBox {
      spacing = 10
      content = List(
        new Label(label){
          prefWidth = 175
        },
        new Slider {
          prefWidth = 200
          min = mini
          max = maxi
          value <==> model
          snapToTicks = true
          blockIncrement = increment
          majorTickUnit = increment
        },
        new Label {
          text <== model.asString + " " + unitLabel
          prefWidth = 100
        },
        new Button("Apply") {
          onAction = handle { apply() }
        },
        new Button("Default") {
          onAction = handle { reset() }
        }
      )
    }

  def globalReferenceBiasSlider = labeledSnappingSliderGroup("Global Reference Bias", CalibrationController.globalReferenceBias, 0, 3000, 1, "mV", CalibrationController.applyGlobalReferenceBias, CalibrationController.resetGlobalReferenceBias)

  def pixelBiasSlider = labeledSnappingSliderGroup("Pixel Bias Range", CalibrationController.pixelBiasRange, 0, 1500, 1, "mV", CalibrationController.applyPixelBiasRange, CalibrationController.resetPixelBiasRange)

  def integrationTimeSlider = labeledSnappingSliderGroup("Integration Time", CalibrationController.integrationTime, 0, 100, 1, "\u00b5s", CalibrationController.applyIntegrationTime, CalibrationController.resetIntegrationTime)

  def adcDelaySlider = labeledSnappingSliderGroup("Adc Delay", CalibrationController.adcDelay, 0, 9, 1, "/ 10 cycle", CalibrationController.applyAdcDelay, CalibrationController.resetAdcDelay)

  def correctionControls: Node = {
    val correctionMode = new ToggleGroup
    new VBox {
      spacing = 10
      content = List(
        new CheckBox("Enable Correction") {
          selected <==> correctionEnabled
        },
        new RadioButton("1 point") {
          disable <== !correctionEnabled || !Measurement.darkImageSet
          selected <==> onePointCorrection
          toggleGroup = correctionMode
        },
        new RadioButton("2 point") {
          disable <== !correctionEnabled || !Measurement.slopeSet
          selected <==> twoPointCorrection
          toggleGroup = correctionMode
        },
        new Button("Get Dark Image") {
          onAction = handle {
            MeasurementController.captureDarkImage()
          }
        },
        new Button("Get Gray Image") {
          onAction = handle {
            MeasurementController.captureGrayImage()
          }
        }
      )
    }
  }

  def nucControls: Node = new VBox {
    spacing = 10
    content = List(
      nucLabelBox,
      partitionSelector,
      new Button("Calculate and Save") {
        onAction = handle {
          calculateAndApplyNuc()
        }
      }
    )
  }

  def nucLabelBox: Node = new TextField {
    promptText = "Enter NUC Setting Label"
    prefColumnCount = 25
    text <==> nucLabel
  }

  def partitionSelector: Node = new HBox {
    spacing = 10
    content = List(
      new ChoiceBox(flashPartitions) {
        value <==> selectedPartition
      },
      new Label {
        text <==> currentNucLabel
      }
    )
  }

  def bitfileSelector: Node = new ChoiceBox(bitfileLabels) {
    value <==> selectedBitfile
  }

  def measurementControlPanel: Node = new ScrollPane {
    content = new VBox {
      padding = Insets(10)
      spacing = 20
      content = List(
        netd,
        new Separator,
        resistorMap,
        new Separator,
        noise
      )
    }
  }

  def netd: Node = {
    def tempBox(label: Int): Node = new HBox {
      spacing = 10
      content = List(
        new TextField {
          prefColumnCount = 5
          promptText = "Enter T" + label
          text <==> MeasurementController.netdTemp(label)
        },
        new Button("Capture image at " + label) {
          onAction = handle {
            MeasurementController.captureNetdImage(label)
          }
        }
      )
    }

    new VBox {
      spacing = 10
      content = List(
        new TextField {
          prefColumnCount = 5
          text <==> MeasurementController.netdFrames
          promptText = "# frames"
        },
        tempBox(0),
        tempBox(1),
        new Button("Measure NETD") {
          disable <== !MeasurementController.t0Set || !MeasurementController.t1Set
          onAction = handle {
            MeasurementController.measureNetd()
          }
        },
        new HBox {
          spacing = 10
          content = List(
            new Label("F#") {
              prefWidth = 110
            },
            new TextField {
              text <==> MeasurementController.fNumber
            }
          )
        },
        new HBox {
          spacing = 10
          content = List(
            new Label("Dimension (\u00b5m)") {
              prefWidth = 110
            },
            new TextField {
              text <==> MeasurementController.detectorDimension
            }
          )
        },
        responsivity
      )
    }
  }

  def responsivity: Node = new Button("Measure Responsivity") {
    disable <== !MeasurementController.netdDone
    onAction = handle {
      MeasurementController.measureResponsivity()
    }
  }

  def resistorMap: Node = new Button("Resistor Map") {
    onAction = handle {
      MeasurementController.createResistorMap()
      MeasurementController.createReferenceResistorMap()
      FpgaController.disconnectFromFpga()
      FpgaController.connectToFpga()
    }
  }

  def noise: Node = new VBox {
    spacing = 10
    content = List(
      new TextField {
        prefColumnCount = 5
        text <==> MeasurementController.noiseFrames
        promptText = "# frames"
      },
      new Button("Measure Noise") {
        onAction = handle {
          MeasurementController.measureNoise()
        }
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

  def generalControls: Node = new VBox {
    padding = Insets(10)
    spacing = 20
    content = List(
      bitfileSelector,
      connectButton,
      disconnectButton,
      memoryMapButton,
      selfTestModeSelector,
      cmosTestModeSelector,
      nameBox,
      saveButton
    )
  }

  def nameBox: Node = new TextField {
    text <==> Measurement.name
    prefColumnCount = 10
    promptText = "Enter Die Label"
  }

  def saveButton: Node = new Button("Save Results") {
    onAction = handle { MeasurementController.measurement.save("result.json") }
    // TODO: Add file selector for saving results
  }

  def selfTestModeSelector: Node = new CheckBox("Self Test") {
    disable <== deviceConnected
    selected <==> isSelfTest
  }

  def cmosTestModeSelector: Node = new CheckBox("Cmos Test") {
    disable <== !deviceConnected
    selected <==> isCmosTest
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
