import javafx.application.{ Application, Platform }
import javafx.event.{ ActionEvent, Event, EventHandler }
import javafx.geometry.Insets
import javafx.scene.{ Group, Scene }
import javafx.scene.control.Button
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{ BorderPane, HBox }
import javafx.scene.paint.Color
import javafx.scene.shape.{ Rectangle, StrokeType }
import javafx.stage.Stage
import scala.annotation.tailrec
import scala.collection.JavaConversions._

object GameOfLife {

  val CellSize = 16

  val Rows = 20

  val Columns = 40

  val AliveColor = Color.DARKGREEN

  val DeadColor = Color.WHITE

  val MouseEnteredColor = Color.RED

  val uiCells = createUiCells(Rows, Columns)

  @volatile private var isRunning = false

  implicit def funToHandler[A <: Event](f: A => Unit): EventHandler[A] =
    new EventHandler[A] {
      override def handle(event: A): Unit =
        f(event)
    }

  def main(args: Array[String]): Unit =
    Application.launch(classOf[GameOfLife], args: _*)

  private def createRoot = {
    val root = new BorderPane
    root.setCenter(createGrid)
    root.setTop(createButtons)
    root
  }

  private def createGrid = {
    val grid = new Group
    grid.getChildren.addAll(uiCells)
    grid
  }

  private def createButtons = {
    val buttons = new HBox
    buttons.setStyle("-fx-background-color: #d3d3d3")
    buttons.setPadding(new Insets(10, 10, 10, 10))
    buttons.setSpacing(10)

    val run = new Button("Run")
    val stop = new Button("Stop")

    run setOnAction { (_: ActionEvent) =>
      run.setDisable(true)
      val runnable = new Runnable() {
        override def run(): Unit = {
          val initialGeneration = {
            val aliveCells =
              for {
                uiCell <- uiCells if isUiCellAlive(uiCell)
                (x, y) = coordinates(uiCell)
              } yield Cell(x, y)
            new Generation(aliveCells.toSet)
          }
          @tailrec def loop(generation: Generation): Unit =
            if (isRunning) {
              val nextGeneration = generation.next
              uiCells foreach { uiCell =>
                val (x, y) = coordinates(uiCell)
                Platform.runLater(new Runnable() {
                  override def run(): Unit =
                    setUiCellAlive(uiCell, nextGeneration.aliveCells contains Cell(x, y))
                })
              }
              Thread.sleep(500)
              loop(nextGeneration)
            }
          loop(initialGeneration)
        }
      }
      isRunning = true
      new Thread(runnable).start()
      stop.setDisable(false)
    }

    stop.setDisable(true)
    stop setOnAction { (_: ActionEvent) =>
      stop.setDisable(true)
      isRunning = false
      run.setDisable(false)
    }

    buttons.getChildren.addAll(run, stop)
    buttons
  }

  private def createUiCells(rows: Int, columns: Int) =
    for {
      row <- 0 until rows
      column <- 0 until columns
    } yield createUiCell(row, column)

  private def createUiCell(row: Int, column: Int) = {
    val uiCell = new Rectangle(CellSize, CellSize, DeadColor)
    uiCell.setX(column * CellSize)
    uiCell.setY(row * CellSize)
    uiCell.setStrokeType(StrokeType.INSIDE)
    uiCell.setStrokeWidth(2)
    uiCell.setOnMouseClicked((_: MouseEvent) => if (!isRunning) setUiCellAlive(uiCell, !isUiCellAlive(uiCell)))
    uiCell.setOnMouseEntered((_: MouseEvent) => if (!isRunning) uiCell.setStroke(MouseEnteredColor))
    uiCell.setOnMouseExited((_: MouseEvent) => if (!isRunning) uiCell.setStroke(null))
    uiCell
  }

  private def isUiCellAlive(uiCell: Rectangle) =
    uiCell.getFill == AliveColor

  private def setUiCellAlive(uiCell: Rectangle, isAlive: Boolean) =
    uiCell.setFill(if (isAlive) AliveColor else DeadColor)

  private def coordinates(uiCell: Rectangle) =
    (uiCell.getX.toInt / CellSize, uiCell.getY.toInt / CellSize)
}

class GameOfLife extends Application {

  override def start(stage: Stage): Unit = {
    stage.setScene(new Scene(GameOfLife.createRoot))
    stage.show()
  }
}
