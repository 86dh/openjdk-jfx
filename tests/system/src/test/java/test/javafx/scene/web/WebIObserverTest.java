/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import static javafx.concurrent.Worker.State.SUCCEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.util.Util;

public class WebIObserverTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    private static WebIObserverTestApp WebIObserverTestApp;

    private WebView webView;

    public static class WebIObserverTestApp extends Application {
        private Stage primaryStage = null;

        @Override
        public void init() {
            WebIObserverTest.WebIObserverTestApp = this;
        }

        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            this.primaryStage = primaryStage;
            launchLatch.countDown();
        }
    }

    @BeforeAll
    public static void setupOnce() {
        Util.launch(launchLatch, WebIObserverTestApp.class);
    }

    @AfterAll
    public static void tearDownOnce() {
        Util.shutdown();
    }

    @BeforeEach
    public void setupTestObjects() {
        Platform.runLater(() -> {
            webView = new WebView();
            WebIObserverTestApp.primaryStage.setScene(new Scene(webView));
            WebIObserverTestApp.primaryStage.show();
        });
    }

    @Test public void testIO() {
        final CountDownLatch webViewStateLatch = new CountDownLatch(1);
        URL resource = WebIObserverTest.class.getResource("testIObserver.html");
        assertNotNull(resource, "Resource was null");

        Util.runAndWait(() -> {
            assertNotNull(webView);
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    webViewStateLatch.countDown();
                }
            });

            webView.getEngine().load(resource.toExternalForm());
        });

        assertTrue(Util.await(webViewStateLatch), "Timeout waiting for succeeded state");
        Util.sleep(500);

        Util.runAndWait(() ->
                assertEquals("?", getIntersectionRatio(), "Unknown intersection ratio"));

        Util.runAndWait(() -> webView.getEngine().executeScript("testIO()"));
        Util.sleep(100);

        Util.runAndWait(() ->
                assertEquals("0.5", getIntersectionRatio(), "Intersection ratio"));
    }

    private String getIntersectionRatio() {
        Object object = webView.getEngine().executeScript("document.querySelector('#output pre').innerText");
        assertNotNull(object, "InnerText was null");
        return String.valueOf(object);
    }
}
