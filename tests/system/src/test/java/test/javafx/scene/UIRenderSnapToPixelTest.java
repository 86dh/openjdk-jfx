/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.javafx.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.sun.javafx.PlatformUtil;
import test.util.Util;

public class UIRenderSnapToPixelTest {
    private static final double scale = 1.25;
    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static volatile Stage stage;
    private static final double epsilon = 0.00001;

    @BeforeAll
    public static void setupOnce() throws Exception {
        System.setProperty("glass.win.uiScale", String.valueOf(scale));
        System.setProperty("glass.gtk.uiScale", String.valueOf(scale));

        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void teardown() {
        Util.shutdown();
    }

    @Test
    public void testScrollPaneSnapChildrenToPixels() {
        assumeTrue(PlatformUtil.isLinux() || PlatformUtil.isWindows());

        assertEquals(scale, stage.getRenderScaleY(), 0.0001, "Wrong render scale");

        for (Node node : stage.getScene().getRoot().getChildrenUnmodifiable()) {
            if (node instanceof ScrollPane) {
                var sp = (ScrollPane) node;
                assertEquals(0, ((sp.snappedTopInset() * scale) + epsilon) % 1, 0.0001, "Top inset not snapped to pixel");
                assertEquals(0, ((sp.snappedBottomInset() * scale) + epsilon) % 1, 0.0001, "Bottom inset not snapped to pixel");
                assertEquals(0, ((sp.snappedLeftInset() * scale) + epsilon) % 1, 0.0001, "Left inset not snapped to pixel");
                assertEquals(0, ((sp.snappedRightInset() * scale) + epsilon) % 1, 0.0001, "Right inset not snapped to pixel");
            }
        }
    }

    public static class TestApp extends Application {
        private static void run() {
            startupLatch.countDown();
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            final Label label = new Label("This text may appear blurry at some screen scale without the fix for JDK-8211294");
            final ScrollPane scrollpane = new ScrollPane(label);
            scrollpane.setSnapToPixel(true);
            final VBox root = new VBox();
            root.getChildren().add(new Label("This text should be sharp at all screen scale"));
            root.getChildren().add(scrollpane);
            final Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            stage = primaryStage;
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> Platform.runLater(TestApp::run));
            stage.show();
        }
    }

}
