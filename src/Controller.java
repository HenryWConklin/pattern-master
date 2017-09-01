import com.sun.corba.se.impl.orbutil.graph.Graph;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Controller {
    @FXML private Stage mainStage;
    @FXML private Canvas imageCanvas;
    @FXML private Canvas overlayCanvas;
    @FXML private TextField browseText;
    @FXML private Canvas gridCanvas;
    @FXML private Spinner<Integer> gridRowsSpinner;
    @FXML private Spinner<Integer> gridColumnsSpinner;
    @FXML private Slider overlaySizeSlider;
    @FXML private Slider overlayRotationSlider;
    @FXML private ColorPicker color1Picker;
    @FXML private ColorPicker color2Picker;

    private Image image;

    private GraphicsContext imageGC;
    private GraphicsContext overlayGC;
    private GraphicsContext gridGC;

    private double imageTransX, imageTransY;

    @FXML void initialize() {
        imageTransX = 0;
        imageTransY = 0;

        SpinnerValueFactory<Integer> factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 30);
        gridRowsSpinner.setValueFactory(factory);
        TextFormatter formatter = new TextFormatter(factory.getConverter(), factory.getValue());
        gridRowsSpinner.getEditor().setTextFormatter(formatter);
        factory.valueProperty().bindBidirectional(formatter.valueProperty());
        gridRowsSpinner.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observableValue, Integer o, Integer t1) {
                Controller.this.redrawOverlay();
            }
        });

        factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE, 30);
        gridColumnsSpinner.setValueFactory(factory);
        formatter = new TextFormatter(factory.getConverter(), factory.getValue());
        gridColumnsSpinner.getEditor().setTextFormatter(formatter);
        factory.valueProperty().bindBidirectional(formatter.valueProperty());
        gridColumnsSpinner.valueProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> observableValue, Integer o, Integer t1) {
                Controller.this.redrawOverlay();
            }
        });

        overlaySizeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                Controller.this.redrawImage();
            }
        });

        overlayRotationSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                Controller.this.redrawImage();
            }
        });
        imageGC = imageCanvas.getGraphicsContext2D();
        imageGC.save();

        overlayGC = overlayCanvas.getGraphicsContext2D();

        gridGC = gridCanvas.getGraphicsContext2D();


        color1Picker.valueProperty().addListener(new ChangeListener<Color>() {
            @Override
            public void changed(ObservableValue<? extends Color> observableValue, Color color, Color t1) {
                Controller.this.redrawGrid();
            }
        });

        color2Picker.setValue(Color.BLACK);
        color2Picker.valueProperty().addListener(new ChangeListener<Color>() {
            @Override
            public void changed(ObservableValue<? extends Color> observableValue, Color color, Color t1) {
                Controller.this.redrawGrid();
            }
        });


    }

    @FXML void openFile(ActionEvent e) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File f = fileChooser.showOpenDialog(mainStage);
        if (f!= null)
        try {
            image = new Image(new FileInputStream(f));
            browseText.setText(f.getName());

            imageGC.restore();
            imageGC.save();
            imageGC.clearRect(0,0, imageCanvas.getWidth(), imageCanvas.getHeight());
            imageTransX = 0;
            imageTransY = 0;
            overlayRotationSlider.setValue(0);
            overlaySizeSlider.setValue(0);
            redrawImage();
            redrawOverlay();

            overlaySizeSlider.setValue(0);

        } catch (FileNotFoundException e1) {
            System.err.println("Could not open image file");
            e1.printStackTrace();
        }
    }

    
    private static final double OVERLAY_MAX_DIM = 300;
    private void redrawOverlay() {
        int rows = (Integer)gridRowsSpinner.getValue();
        int cols = (Integer)gridColumnsSpinner.getValue();
        int bigDim = (rows > cols ? rows : cols);
        double width = (double)cols/bigDim * OVERLAY_MAX_DIM;
        double height = (double)rows/bigDim * OVERLAY_MAX_DIM;

        double midX = overlayCanvas.getWidth()/2;
        double midY = overlayCanvas.getHeight()/2;

        double maxX = midX + width/2;
        double minX = midX - width/2;
        double maxY = midY + height/2;
        double minY = midY - height/2;

        overlayGC.clearRect(0,0,overlayCanvas.getWidth(), overlayCanvas.getHeight());
        overlayGC.setStroke(Paint.valueOf("red"));
        for (double x = minX; x <= maxX + 1e-10; x += width/cols) {
            overlayGC.strokeLine(x, minY, x, maxY);
        }
        for (double y = minY; y <= maxY + 1e-10; y+= height/rows) {
            overlayGC.strokeLine(minX, y, maxX, y);
        }

        redrawGrid();
    }

    private void redrawGrid() {
        gridGC.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());


        int rows = (Integer) gridRowsSpinner.getValue();
        int cols = (Integer) gridColumnsSpinner.getValue();
        int bigDim = (rows > cols ? rows : cols);
        double width = (cols) * ((gridCanvas.getWidth() - 30) / bigDim);
        double height = (rows) * ((gridCanvas.getHeight() - 30) / bigDim);
        double imageBoxSize = (OVERLAY_MAX_DIM / bigDim);
        double gridBoxSize = ((gridCanvas.getWidth() - 30) / bigDim);

        double imgwidth = (double) cols / bigDim * OVERLAY_MAX_DIM;
        double imgheight = (double) rows / bigDim * OVERLAY_MAX_DIM;
        double imgCornerX = (overlayCanvas.getWidth() - imgwidth) / 2;
        double imgCornerY = (overlayCanvas.getHeight() - imgheight) / 2;


        double maxX = gridCanvas.getWidth();
        double minX = maxX - width;
        double maxY = gridCanvas.getHeight();
        double minY = maxY - height;

        WritableImage transImg = imageCanvas.snapshot(null, null);
        PixelReader preader = transImg.getPixelReader();
        Color color1 = color1Picker.getValue();
        Color color2 = color2Picker.getValue();

        ////Dithering matrix
        int[][] mat = {
                {1,9,3,11},
                {13,5,15,7},
                {4,12,2,10},
                {16,8,14,6},
        };
        double div = 1.0/17;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Point3D avgColor = new Point3D(0,0,0);
                int tot = 0;
                for (double i = r * imageBoxSize; i < (r + 1) * imageBoxSize; i++) {
                    for (double j = c * imageBoxSize; j < (c + 1) * imageBoxSize; j++) {
                        Color color = preader.getColor((int)(j + imgCornerX), (int)(i + imgCornerY));
                        avgColor = avgColor.add(color.getRed(), color.getGreen(), color.getBlue());
                        tot++;
                    }
                }
                avgColor = avgColor.multiply(1.0/tot);

                avgColor = avgColor.add(avgColor.multiply(mat[r%mat.length][c%mat[0].length] * div));

                Color color = closestColor(avgColor, color1, color2);
                gridGC.setFill(color);
                gridGC.fillRect(minX + c * gridBoxSize, minY + r * gridBoxSize, gridBoxSize, gridBoxSize);

            }
        }

        gridGC.setLineWidth(1);
        gridGC.setStroke(Color.BLACK);
        for (int r = 0; r <= rows; r++) {
            if (r==0 || r % 5 == 4) {
                gridGC.strokeText("" + (r + 1), minX - 30, minY + (r+1) * gridBoxSize - 5, 30);
                gridGC.strokeLine(minX-30, (int)(minY + (r+1) * gridBoxSize)+.5, minX, (int)(minY + (r+1) * gridBoxSize)+.5);
            }
            gridGC.strokeLine(minX, (int)(minY + r * gridBoxSize) + .5, maxX, (int)(minY + r * gridBoxSize) + .5);
        }
        for (int c = 0; c <= cols; c++) {
            if (c == 0 || c % 5 == 4) {
                gridGC.strokeText("" + (c + 1), minX + (c) * gridBoxSize, minY - 5);
                gridGC.strokeLine((int)(minX + c * gridBoxSize) + .5, minY-30, (int)(minX + c *gridBoxSize) + .5, maxY);
            }
            gridGC.strokeLine((int)(minX + c * gridBoxSize) + .5, minY, (int)(minX + c * gridBoxSize) + .5, maxY);
        }

    }

    private Color closestColor(Point3D color, Color color1, Color color2) {
        Point3D diff1 = new Point3D(color.getX() - color1.getRed(), color.getY() - color1.getGreen(), color.getZ() - color1.getBlue());
        Point3D diff2 = new Point3D(color.getX() - color2.getRed(), color.getY() - color2.getGreen(), color.getZ() - color2.getBlue());
        return (diff1.dotProduct(diff1) < diff2.dotProduct(diff2) ? color1 : color2);
    }

    @FXML public void saveGrid(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Pattern");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("png", "*.png"),
                new FileChooser.ExtensionFilter("jpg", "*.jpg")
        );

        File f = fileChooser.showSaveDialog(mainStage);

        if (f != null) {
            if (!f.toString().toLowerCase().endsWith("." + fileChooser.getSelectedExtensionFilter().getDescription())) {
                f = new File(f.toString() + "." + fileChooser.getSelectedExtensionFilter().getDescription());
            }
            try {
                WritableImage img  = gridCanvas.snapshot(null, null);
                BufferedImage bImg = SwingFXUtils.fromFXImage(img, null);
                ImageIO.write(bImg, fileChooser.getSelectedExtensionFilter().getDescription(), f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private double dragX,dragY;
    @FXML public void translateImage(MouseEvent mouseEvent) {
        if (mouseEvent.isPrimaryButtonDown()) {
            imageTransX += mouseEvent.getSceneX() - dragX;
            imageTransY += mouseEvent.getSceneY() - dragY;
            dragX = mouseEvent.getSceneX();
            dragY = mouseEvent.getSceneY();
            redrawImage();
            mouseEvent.consume();
        }
    }

    private void redrawImage() {
        if (image != null) {
            imageGC.restore();
            imageGC.save();
            imageGC.clearRect(0, 0, imageCanvas.getWidth(), imageCanvas.getHeight());
            Point2D mid = new Point2D(image.getWidth() / 2, image.getHeight() / 2);
            double imageScale = Math.pow(2, overlaySizeSlider.getValue());

            imageGC.translate(imageTransX, imageTransY);
            imageGC.translate(mid.getX(), mid.getY());
            imageGC.rotate(overlayRotationSlider.getValue());
            imageGC.scale(imageScale, imageScale);
            imageGC.translate(-mid.getX(), -mid.getY());

            imageGC.drawImage(image, 0, 0);
            redrawGrid();
        }
    }

    @FXML public void imageCanvasOnClick(MouseEvent mouseEvent) {
        if (mouseEvent.isPrimaryButtonDown()) {
            dragX = mouseEvent.getSceneX();
            dragY = mouseEvent.getSceneY();
            mouseEvent.consume();
        }
    }
}
