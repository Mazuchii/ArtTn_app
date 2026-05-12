package tn.esprit.museum.utils;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public final class WindowManager {
    private WindowManager() {
    }

    public static void applyStandardSize(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
    }

    public static void applyDashboardFill(Parent root, boolean removeNestedSidebar) {
        if (root == null) {
            return;
        }

        AnchorPane.setLeftAnchor(root, 0.0);
        AnchorPane.setRightAnchor(root, 0.0);
        AnchorPane.setTopAnchor(root, 0.0);
        AnchorPane.setBottomAnchor(root, 0.0);
        VBox.setVgrow(root, Priority.ALWAYS);
        HBox.setHgrow(root, Priority.ALWAYS);

        if (root instanceof Region region) {
            region.setMinSize(0, 0);
            region.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }

        if (removeNestedSidebar && root instanceof BorderPane borderPane) {
            borderPane.setLeft(null);
            if (borderPane.getCenter() instanceof Region centerRegion) {
                centerRegion.setMinSize(0, 0);
                centerRegion.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }
        }
    }

    public static void applyWindowState(Stage stage, boolean wasMaximized, boolean wasFullScreen, boolean forceFullScreen) {
        if (stage == null) {
            return;
        }

        boolean shouldMaximize = wasMaximized || forceFullScreen;
        boolean shouldFullScreen = wasFullScreen || forceFullScreen;
        applyState(stage, shouldMaximize, shouldFullScreen);
        Platform.runLater(() -> applyState(stage, shouldMaximize, shouldFullScreen));
    }

    private static void applyState(Stage stage, boolean maximized, boolean fullScreen) {
        stage.setMaximized(maximized);
        if (fullScreen) {
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
        }
    }
}
