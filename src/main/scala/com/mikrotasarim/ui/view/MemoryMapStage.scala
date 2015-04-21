package com.mikrotasarim.ui.view

import com.mikrotasarim.ui.model.MemoryMap
import com.mikrotasarim.ui.model.MemoryMap.MemoryLocation

import scalafx.Includes._
import scalafx.scene._
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.stage.Stage

object MemoryMapStage extends Stage {

  width = 400
  height = 600
  title = "Memory Map"
  scene = new Scene() {
    root = new ScrollPane {
      content = new VBox {
        content = MemoryMap.memoryLocations.map(CreateMemoryLocationControl)
      }
    }
  }

  def CreateMemoryLocationControl(model: MemoryLocation): Node = new HBox {
    spacing = 10
    content = List(
      new Label("Addr: " + model.address) {
        prefWidth = 75
      },
      new TextField {
        text <==> model.text
      },
      new Button("Commit") {
        onAction = handle {
          model.commit()
        }
      }
    )
  }
}
