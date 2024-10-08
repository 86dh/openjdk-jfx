/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.util.Util;

/*
 * Test for verifying DatePicker update on closing the Alert dialog.
 *
 * There are 2 tests in this file.
 * Steps for testDatePickerUpdateOnAlertCloseUsingMouse()
 * 1. Create an alert dialog and add date picker to it.
 * 2. Add button to scene and show alert dialog on button click.
 * 3. Click on date picker and select a date from popup.
 * 4. Save the selected date locally.
 * 5. Click on button and alert to display date picker again.
 * 6. Select another date from the popup and save locally.
 * 5. Verify that 2 date selected are not same and
 *    selected dates are updated in the date picker.
 *
 * Steps for testDatePickerUpdateOnAlertCloseUsingKeyboard()
 * 1. Create an alert dialog and add date picker to it.
 * 2. Add button to scene and show alert dialog on button click.
 * 3. Click on date picker and select a date from popup using keyboard.
 * 4. Verify that selected date is updated in the date picker.
 */
public class DatePickerUpdateOnAlertCloseTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static CountDownLatch onDatePickerShownLatch = new CountDownLatch(1);
    static CountDownLatch onAlertShownLatch = new CountDownLatch(1);
    static CountDownLatch onAlertHiddenLatch = new CountDownLatch(1);
    static Robot robot;

    static volatile Stage stage;
    static volatile Scene scene;
    static Button button;

    static final int SCENE_WIDTH = 250;
    static final int SCENE_HEIGHT = SCENE_WIDTH;
    static final int Y_FACTOR = 5;

    DatePicker datePicker;
    Alert dialog;

    private void mouseClick(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (scene.getWindow().getX() + scene.getX() + x),
                                (int) (scene.getWindow().getY() + scene.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    private void mouseClickOnAlertDialog(double x, double y) {
        Util.runAndWait(() -> {
            robot.mouseMove((int) (dialog.getX() + x), (int) (dialog.getY() + y));
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
    }

    private void selectDatePicker() throws Exception {
        mouseClickOnAlertDialog(datePicker.getLayoutX() + datePicker.getWidth() - 15,
                                    datePicker.getLayoutY() + datePicker.getHeight() / 2);
        Thread.sleep(800); // Wait for DatePicker popup to display.
        Util.waitForLatch(onDatePickerShownLatch, 5, "Failed to show DatePicker popup.");
    }

    private void showAlertDialog() throws Exception {
        mouseClick(button.getLayoutX() + button.getWidth() / 2,
                    button.getLayoutY() + button.getHeight() / 2);
        Thread.sleep(400); // Wait for Alert dialog to display.
        Util.waitForLatch(onAlertShownLatch, 5, "Failed to show Alert dialog.");
    }

    private void selectNextDate() {
        Util.runAndWait(() -> {
            robot.keyType(KeyCode.RIGHT);
            robot.keyType(KeyCode.ENTER);
        });
        Util.waitForLatch(onAlertHiddenLatch, 5, "Failed to hide Alert dialog.");
    }

    @Test
    public void testDatePickerUpdateOnAlertCloseUsingMouse() throws Exception {
        Thread.sleep(1000); // Wait for stage to layout

        showAlertDialog();
        selectDatePicker();

        mouseClick(datePicker.getLayoutX() + datePicker.getWidth() / 2,
                    datePicker.getLayoutY() + datePicker.getHeight() * Y_FACTOR);
        LocalDate oldDate = datePicker.getValue();

        showAlertDialog();
        selectDatePicker();

        mouseClick(datePicker.getLayoutX() + datePicker.getWidth() / 3,
                    (datePicker.getLayoutY() + datePicker.getHeight() * Y_FACTOR));
        LocalDate newDate = datePicker.getValue();

        Thread.sleep(400); // Wait for date to be selected.
        Assertions.assertNotEquals(oldDate, newDate);
    }

    @Test
    public void testDatePickerUpdateOnAlertCloseUsingKeyboard() throws Exception {
        Thread.sleep(1000); // Wait for stage to layout

        showAlertDialog();
        selectDatePicker();
        selectNextDate();

        Thread.sleep(400); // Wait for date to be selected.
        Assertions.assertFalse(LocalDate.now().isEqual(datePicker.getValue()));
    }

    @AfterEach
    public void resetUI() {
        Util.runAndWait(() -> {
            datePicker.setOnShown(null);
            datePicker.setOnAction(null);
            dialog.setOnShown(null);
            dialog.setOnHidden(null);
            button.setOnAction(null);
        });
    }

    @BeforeEach
    public void setupUI() {
        Util.runAndWait(() -> {
            datePicker = new DatePicker(LocalDate.now());
            datePicker.setOnShown(event -> {
                onDatePickerShownLatch.countDown();
            });
            datePicker.valueProperty().addListener(event -> {
                dialog.close();
            });

            dialog = new Alert(AlertType.INFORMATION);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initOwner(stage);
            dialog.setOnShown(event -> {
                onAlertShownLatch.countDown();
            });

            dialog.setOnHidden(event -> {
                onAlertHiddenLatch.countDown();
            });

            button.setOnAction(event -> {
                dialog.getDialogPane().setContent(datePicker);
                dialog.show();
            });
        });
    }

    @BeforeAll
    public static void initFX() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void exit() {
        Util.shutdown();
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            robot = new Robot();
            stage = primaryStage;

            button = new Button("Show dialog");
            scene = new Scene(button, SCENE_WIDTH, SCENE_HEIGHT);

            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.show();
        }
    }
}
