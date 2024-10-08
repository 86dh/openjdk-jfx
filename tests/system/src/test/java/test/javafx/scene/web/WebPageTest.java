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
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebEngineShim;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;
import test.util.Util;

public class WebPageTest {
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Maintain one application instance
    static WebPageTestApp webPageTestApp;

    private WebView webView;

    public static class WebPageTestApp extends Application {
        Stage primaryStage = null;

        @Override
        public void init() {
            WebPageTest.webPageTestApp = this;
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
        Util.launch(launchLatch, WebPageTestApp.class);
    }

    @AfterAll
    public static void tearDownOnce() {
        Util.shutdown();
    }

    @BeforeEach
    public void setupTestObjects() {
        Platform.runLater(() -> {
            webView = new WebView();
            webPageTestApp.primaryStage.setScene(new Scene(webView));
            webPageTestApp.primaryStage.show();
        });
    }

    /**
     * @test
     * @bug 8260257
     * summary Checks if scrolling is possible
     */
    @Test public void testScroll() {
        final CountDownLatch webViewStateLatch = new CountDownLatch(1);
        final String htmlContent = "\n"
            + "<html>\n"
            + "<body style='height:1500px'>\n"
            + "<p id='test'>Fail</p>\n"
            + "<script>\n"
            + "window.onscroll = function() {scrollFunc()};\n"
            + "function scrollFunc() {\n"
            + "document.getElementById('test').innerHTML = 'Pass';\n"
            + "}\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>";

        Util.runAndWait(() -> {
            assertNotNull(webView);
            webView.getEngine().getLoadWorker().stateProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue == SUCCEEDED) {
                    webView.requestFocus();
                }
            });

            webView.focusedProperty().
                addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    webViewStateLatch.countDown();
                }
            });

            webView.getEngine().loadContent(htmlContent);
        });

        assertTrue(Util.await(webViewStateLatch), "Timeout when waiting for focus change ");
        Util.sleep(1000);

        Util.runAndWait(() -> {
            final WebPage page = WebEngineShim.getPage(webView.getEngine());
            assertNotNull(page);
            WebPageShim.scroll(page, 1, 1, 0, 100);
        });

        Util.sleep(500);

        Util.runAndWait(() -> {
            assertEquals("Pass", webView.getEngine().executeScript("document.getElementById('test').innerHTML"), "WebPage should display pass: ");
        });
    }
}
