/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.tk.Toolkit;
import javafx.scene.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;
import static java.util.stream.Collectors.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.TextInputControlShim;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.control.skin.TextInputSkinShim;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import test.com.sun.javafx.scene.control.infrastructure.KeyEventFirer;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

public class TextFieldTest {
    private TextField txtField;//Empty string
    private TextField dummyTxtField;//With string value

    @BeforeEach
    public void setup() {
        txtField = new TextField();
        dummyTxtField = new TextField("dummy");
        setUncaughtExceptionHandler();
    }

    private void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

    private void removeUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    /*********************************************************************
     * Tests for the constructors                                        *
     ********************************************************************/

    @Test public void defaultConstructorShouldHaveEmptyString() {
        assertEquals("", txtField.getText());
    }

    @Test public void oneStrArgConstructorShouldHaveString() {
        assertEquals("dummy", dummyTxtField.getText());
    }

    /*********************************************************************
     * Tests for the null checks                                         *
     ********************************************************************/

    @Test public void checkContentNotNull() {
        assertNotNull(TextInputControlShim.getContent(txtField));
    }

    @Test public void checkCharNotNull() {
        assertNotNull(txtField.getCharacters());
    }

    @Test public void checkDefPromptTextEmptyString() {
        assertEquals("", txtField.getPromptText());
    }

    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    @Test public void checkDefaultColCount() {
        assertEquals(TextField.DEFAULT_PREF_COLUMN_COUNT, 12);
    }

    @Test public void defaultActionHandlerIsNotDefined() {
        assertNull(txtField.getOnAction());
    }

    @Test public void defaultConstructorShouldSetStyleClassTo_textfield() {
        assertStyleClassContains(txtField, "text-field");
    }

    @Test public void checkCharsSameAsText() {
        assertEquals(dummyTxtField.getCharacters().toString(), dummyTxtField.getText());
    }

    @Test public void checkCharsSameAsContent() {
        assertEquals(dummyTxtField.getCharacters().toString(), TextInputControlShim.getContent_get(dummyTxtField, 0, dummyTxtField.getLength()).toString());
    }

    @Test public void checkTextSameAsContent() {
        assertEquals(dummyTxtField.getText(), TextInputControlShim.getContent_get(dummyTxtField, 0, dummyTxtField.getLength()));
    }

    @Test public void checkPromptTextPropertyName() {
        assertTrue(txtField.promptTextProperty().getName().equals("promptText"));
    }

    @Test public void prefColCountCannotBeNegative() {
        try {
            txtField.setPrefColumnCount(-1);
            fail("Prefcoulumn count cannot be null");//This is non reachable ode if everything goes fine(i.e Exception is thrown)
        } catch(IllegalArgumentException iae) {
            assertNotNull(iae);
        }
    }


    @Test public void oneArgStrConstructorShouldSetStyleClassTo_textfield() {
        assertStyleClassContains(dummyTxtField, "text-field");
    }

    @Test public void checkTextSetGet() {
        dummyTxtField.setText("junk");
        assertEquals(dummyTxtField.getText(), "junk");
    }

    /*********************************************************************
     * Tests for CSS                                                     *
     ********************************************************************/

    @Test public void prefColumnCountSetFromCSS() {
        txtField.setStyle("-fx-pref-column-count: 100");
        Scene s = new Scene(txtField);
        txtField.applyCss();
        assertEquals(100.0, txtField.getPrefColumnCount(), 0);
    }

    @Test public void pseudoClassState_isReadOnly() {
        StageLoader sl = new StageLoader(txtField);
        txtField.applyCss();

        txtField.setEditable(false);
        ObservableSet<PseudoClass> pcSet = txtField.getPseudoClassStates();
        boolean match = false;
        for (PseudoClass pc : pcSet) {
            if (match) break;
            match = "readonly".equals(pc.getPseudoClassName());
        }
        assertTrue(match);

        sl.dispose();
    }

    @Test public void pseudoClassState_isNotReadOnly() {
        StageLoader sl = new StageLoader(txtField);
        txtField.applyCss();

        txtField.setEditable(true);
        ObservableSet<PseudoClass> pcSet = txtField.getPseudoClassStates();
        boolean match = false;
        for (PseudoClass pc : pcSet) {
            if (match) break;
            match = "readonly".equals(pc.getPseudoClassName());
        }
        assertFalse(match);
        sl.dispose();
    }

    /*********************************************************************
     * Tests for property binding                                        *
     ********************************************************************/

    @Test public void checkPromptTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        txtField.promptTextProperty().bind(strPr);
        assertTrue(txtField.getPromptText().equals("value"), "PromptText cannot be bound");
        strPr.setValue("newvalue");
        assertTrue(txtField.getPromptText().equals("newvalue"), "PromptText cannot be bound");
    }

    @Test public void checkTextPropertyBind() {
        StringProperty strPr = new SimpleStringProperty("value");
        txtField.textProperty().bind(strPr);
        assertEquals(txtField.getText(), "value", "Text cannot be bound");
        strPr.setValue("newvalue");
        assertEquals(txtField.getText(),  "newvalue", "Text cannot be bound");
    }

    @Test public void checkOnActionPropertyBind() {
        ObjectProperty<EventHandler<ActionEvent>> op= new SimpleObjectProperty<>();
        EventHandler<ActionEvent> ev = event -> {
            //Nothing to do
        };
        op.setValue(ev);
        txtField.onActionProperty().bind(op);
        assertEquals(ev, op.getValue());
    }

    @Test
    public void testPromptTextWithBindingWithLineBreaks() {
        initStage();
        txtField.setSkin(new TextFieldSkin(txtField));
        String promptWithLineBreaks = "Prompt\nwith\nLineBreaks";
        StringProperty promptProperty = new SimpleStringProperty(promptWithLineBreaks);
        txtField.promptTextProperty().bind(promptProperty);
        root.getChildren().add(txtField);
        Text promptNode = TextInputSkinShim.getPromptNode(txtField);
        assertEquals(promptWithLineBreaks.replace("\n",""), promptNode.getText());
        txtField.promptTextProperty().unbind();
    }

    @Test
    public void testPromptTextWithBindingWithoutLineBreaks() {
        initStage();
        txtField.setSkin(new TextFieldSkin(txtField));
        String promptWithoutLineBreaks = "Prompt without LineBreaks";
        StringProperty promptProperty = new SimpleStringProperty(promptWithoutLineBreaks);
        txtField.promptTextProperty().bind(promptProperty);
        root.getChildren().add(txtField);
        Text promptNode = TextInputSkinShim.getPromptNode(txtField);
        assertEquals(promptWithoutLineBreaks, promptNode.getText());
        txtField.promptTextProperty().unbind();
    }

    @Test
    public void testPromptTextWhenSettingValueWithLineBreaks() {
        initStage();
        txtField.setSkin(new TextFieldSkin(txtField));
        String promptWithoutLineBreaks = "Prompt without LineBreaks";
        String promptWithLineBreaks = "Prompt\nwith\nLineBreaks";
        txtField.setPromptText(promptWithoutLineBreaks);
        root.getChildren().add(txtField);
        Text promptNode = TextInputSkinShim.getPromptNode(txtField);
        assertEquals(promptWithoutLineBreaks, promptNode.getText());
        txtField.setPromptText(promptWithLineBreaks);
        assertEquals(promptWithLineBreaks.replace("\n",""), promptNode.getText());
    }

    @Test
    public void testPromptTextWithNullValue() {
        initStage();
        txtField.setSkin(new TextFieldSkin(txtField));
        String promptWithNull = null;
        StringProperty promptPropertyNull = new SimpleStringProperty(promptWithNull);
        txtField.promptTextProperty().bind(promptPropertyNull);
        root.getChildren().add(txtField);
        Text promptNode = TextInputSkinShim.getPromptNode(txtField);
        assertNull(promptNode);
    }

    /*********************************************************************
     * Miscellaneous Tests                                               *
     ********************************************************************/
    @Test public void lengthMatchesStringLengthExcludingControlCharacters() {
        final String string = "Hello\n";
        txtField.setText(string);
        assertEquals(string.length()-1, txtField.getLength());
    }

    @Test public void prefColumnCountPropertyHasBeanReference() {
        assertSame(txtField, txtField.prefColumnCountProperty().getBean());
    }

    @Test public void prefColumnCountPropertyHasName() {
        assertEquals("prefColumnCount", txtField.prefColumnCountProperty().getName());
    }

    @Test public void onActionPropertyHasBeanReference() {
        assertSame(txtField, txtField.onActionProperty().getBean());
    }

    @Test public void onActionPropertyHasName() {
        assertEquals("onAction", txtField.onActionProperty().getName());
    }

    @Test public void setPromptTextAndSeeValueIsReflectedInModel() {
        txtField.setPromptText("tmp");
        assertEquals(txtField.promptTextProperty().getValue(), "tmp");
    }

    @Test public void setPromptTextAndSeeValue() {
        txtField.setPromptText("tmp");
        assertEquals(txtField.getPromptText(), "tmp");
    }

    @Test public void setTextAndSeeValueIsReflectedInModel() {
        txtField.setText("tmp");
        assertEquals(txtField.textProperty().getValue(), txtField.getText());
    }

    @Test public void setTextAndSeeValue() {
        txtField.setText("tmp");
        assertEquals(txtField.getText() , "tmp");
    }

    @Test public void setPrefColCountAndSeeValueIsReflectedInModel() {
        txtField.setPrefColumnCount(10);
        assertEquals(txtField.prefColumnCountProperty().get(), 10.0, 0.0);
    }

    @Test public void setPrefColCountAndSeeValue() {
        txtField.setPrefColumnCount(10);
        assertEquals(txtField.getPrefColumnCount(), 10.0 ,0.0);
    }

    @Test public void insertAndCheckSubRangeInText() {
        TextInputControlShim.getContent_insert(dummyTxtField, 0, "x", true);
        assertEquals("x", dummyTxtField.getText().substring(0,1));
    }

    @Test public void insertAndCheckSubRangeInContent() {
        TextInputControlShim.getContent_insert(dummyTxtField, 0, "x", true);
        assertEquals("x", TextInputControlShim.getContent_get(dummyTxtField, 0, 1));
    }

    @Test public void appendAndCheckSubRangeInText() {
        dummyTxtField.appendText("x");
        assertEquals("x", dummyTxtField.getText().substring(dummyTxtField.getLength() - 1,dummyTxtField.getLength()));
    }

    @Test public void appendAndCheckSubRangeInContent() {
        dummyTxtField.appendText("x");
        assertEquals("x", TextInputControlShim.getContent_get(dummyTxtField, dummyTxtField.getLength() - 1,dummyTxtField.getLength()));
    }

    @Test public void deleteAndCheckText() {
        TextInputControlShim.getContent_insert(dummyTxtField, 0, "x", false);
        TextInputControlShim.getContent_delete(dummyTxtField, 1, dummyTxtField.getLength(), true);
        assertEquals("x", dummyTxtField.getText());
    }

    private Scene scene;
    private Stage stage;
    private StackPane root;

    /**
     * Guard against potential regression of JDK-8145515: eventFilter
     * on editor not notified for ENTER released.
     */
    @Test
    public void testEditorInComboBoxEnterReleasedFilter() {
        initStage();
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        root.getChildren().add(combo);
        stage.show();
        List<Event> events = new ArrayList<>();
        combo.getEditor().addEventFilter(KEY_RELEASED, events::add);
        KeyCode key = ENTER;
        KeyEventFirer keyFirer = new KeyEventFirer(combo);
        keyFirer.doKeyPress(key);
        assertEquals(1, events.size());
    }

    /**
     * Unfixed part of JDK-8145515, reported as regression JDK-8229914: eventFilter
     * on editor not notified for ENTER pressed.
     */
    @Disabled("JDK-8229914")
    @Test
    public void testEditorInComboBoxEnterPressedFilter() {
        initStage();
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        root.getChildren().add(combo);
        stage.show();
        List<Event> events = new ArrayList<>();
        combo.getEditor().addEventFilter(KEY_PRESSED, events::add);
        KeyCode key = ENTER;
        KeyEventFirer keyFirer = new KeyEventFirer(combo);
        keyFirer.doKeyPress(key);
        assertEquals(1, events.size());
    }

    /**
     * Test related to https://bugs.openjdk.org/browse/JDK-8207759
     * broken event dispatch sequence by forwardToParent.
     */
    @Test
    public void testEventSequenceEnterHandler() {
        initStage();
        root.getChildren().add(txtField);
        stage.show();
        List<Event> events = new ArrayList<>();
        EventHandler<KeyEvent> adder = events::add;
        scene.addEventHandler(KEY_PRESSED, adder);
        root.addEventHandler(KEY_PRESSED, adder);
        txtField.addEventHandler(KEY_PRESSED, adder);
        KeyCode key = ENTER;
        KeyEventFirer keyFirer = new KeyEventFirer(txtField);
        keyFirer.doKeyPress(key);
        assertEquals(3, events.size(), "event count");
        List<Object> sources = events.stream()
                .map(e -> e.getSource())
                .collect(toList());
        List<Object> expected = List.of(txtField, root, scene);
        assertEquals(expected, sources);
    }

    @Test
    public void testEventSequenceEscapeHandler() {
        initStage();
        root.getChildren().add(txtField);
        stage.show();
        List<Event> events = new ArrayList<>();
        EventHandler<KeyEvent> adder = events::add;
        scene.addEventHandler(KEY_PRESSED, adder);
        root.addEventHandler(KEY_PRESSED, adder);
        txtField.addEventHandler(KEY_PRESSED, adder);
        KeyCode key = ESCAPE;
        KeyEventFirer keyFirer = new KeyEventFirer(txtField);
        keyFirer.doKeyPress(key);
        assertEquals(3, events.size(), "event count");
        List<Object> sources = events.stream()
                .map(e -> e.getSource())
                .collect(toList());
        List<Object> expected = List.of(txtField, root, scene);
        assertEquals(expected, sources);
    }


    /**
     * test for JDK-8207774: ENTER must not be forwared if actionHandler
     * consumed the action.
     *
     * Here we test that an accelerator is not triggered.
     */
    @Test
    public void testEnterWithConsumingActionHandlerAccelerator() {
        initStage();
        root.getChildren().add(txtField);
        txtField.addEventHandler(ActionEvent.ACTION, e -> e.consume());
        scene.getAccelerators().put(new KeyCodeCombination(ENTER), () ->
            fail("accelerator must not be notified"));
        stage.show();
        KeyEventFirer keyboard = new KeyEventFirer(txtField);
        keyboard.doKeyPress(ENTER);
    }

    /**
     * test for JDK-8207774: ENTER must not be forwared if actionHandler
     * consumed the action.
     *
     * Here we test that handlers on parent are not notified.
     */
    @Test
    public void testEnterWithConsumingActionHandlerParentHandler() {
        initStage();
        root.getChildren().add(txtField);
        txtField.addEventHandler(ActionEvent.ACTION, e -> e.consume());
        root.addEventHandler(KeyEvent.KEY_PRESSED, e ->
            fail("parent handler must not be notified but received: " + e ));
        stage.show();
        KeyEventFirer keyboard = new KeyEventFirer(txtField);
        keyboard.doKeyPress(ENTER);
    }

    /**
     * sanity: pressing enter actually triggers a consuming actionHandler.
     */
    @Test
    public void testEnterWithConsumingActionHandler() {
        initStage();
        root.getChildren().add(txtField);
        List<ActionEvent> actions = new ArrayList<>();
        txtField.addEventHandler(ActionEvent.ACTION, e -> {
            e.consume();
            actions.add(e);
        });
        stage.show();
        KeyEventFirer keyboard = new KeyEventFirer(txtField);
        keyboard.doKeyPress(ENTER);
        assertEquals(1, actions.size(), "actionHandler must be notified");
        assertTrue(actions.get(0).isConsumed(), "action must be consumed ");
    }

    @Test public void replaceSelectionWithFilteredCharacters() {
        txtField.setText("x xxxyyy");
        txtField.selectRange(2, 5);
        txtField.setTextFormatter(new TextFormatter<>(this::noDigits));
        txtField.replaceSelection("a1234a");
        assertEquals("x aayyy", txtField.getText());
        assertEquals(4, txtField.getSelection().getStart());
        assertEquals(4, txtField.getSelection().getEnd());
    }

    @Test
    public void testTextFormatterWithFilter() {
        txtField.setText("abc");
        txtField.setTextFormatter(new TextFormatter<>(this::upperCase));
        assertEquals("abc", txtField.getText());

        // Set text again to trigger the text formatter filter.
        txtField.setText("abc");
        assertEquals("ABC", txtField.getText());
    }

    @Test
    public void testTextFormatterWithConverter() {
        txtField.setText("200");
        txtField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                // Converter to integer and add 100.
                return super.fromString(value) + 100;
            }
        }));
        // No default value -> text is cleared.
        assertEquals("", txtField.getText());

        txtField.setText("500");
        assertEquals("600", txtField.getText());
    }

    @Test
    public void testTextFormatterWithConverterAndDefaultValue() {
        txtField.setText("200");
        txtField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                // Converter to integer and add 100.
                return super.fromString(value) + 100;
            }
        }, 1000));
        // Default value is set as text.
        assertEquals("1000", txtField.getText());

        txtField.setText("500");
        assertEquals("600", txtField.getText());
    }

    @Test
    public void testTextFormatterWithConverterAndFilter() {
        txtField.setText("200");
        txtField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter() {
            @Override
            public Integer fromString(String value) {
                // Converter to integer and add 100.
                return super.fromString(value) + 100;
            }
        }, 1000, change -> {
            change.setText(change.getText().replace("3", ""));
            return change;
        }));
        // Default value is set as text.
        assertEquals("1000", txtField.getText());

        txtField.setText("500");
        assertEquals("600", txtField.getText());

        // 3 is removed, therefore we get 100. The value converter above will then add 100 (=200).
        txtField.setText("1300");
        assertEquals("200", txtField.getText());
    }

    /**
     * test for JDK-8178368: Right and Center alignment of text field works incorrectly
     *
     * Here we test that text aligns as expected when text width is more than TextField width.
     * All three alignment types LEFT, RIGHT and CENTER are validated below.
     */
    @Test
    public void testTextFieldLeftAlignment() {
        initStage();
        txtField.setSkin(new TextFieldSkin(txtField));
        txtField.setText("A short text.");
        txtField.setPrefColumnCount(20);
        txtField.setAlignment(Pos.BASELINE_LEFT);

        root.getChildren().add(txtField);
        stage.show();

        assertTrue(txtField.getWidth() > TextInputSkinShim.getTextNode(txtField).getLayoutBounds().getWidth());
        assertEquals(TextInputSkinShim.getTextTranslateX(txtField), 0, 0.0);

        txtField.setText("This is a long text. this is  long text.");
        assertTrue(txtField.getWidth() < TextInputSkinShim.getTextNode(txtField).getLayoutBounds().getWidth());
        assertEquals(0, TextInputSkinShim.getTextTranslateX(txtField), 0.0);
    }

    @Test
    public void testTextFieldRightAlignment() {
        initStage();
        txtField.setSkin(new TextFieldSkin(txtField));
        txtField.setText("A short text.");
        txtField.setPrefColumnCount(20);
        txtField.setAlignment(Pos.BASELINE_RIGHT);

        root.getChildren().add(txtField);
        stage.show();

        assertTrue(txtField.getWidth() > TextInputSkinShim.getTextNode(txtField).getLayoutBounds().getWidth());
        assertTrue(TextInputSkinShim.getTextTranslateX(txtField) > 0);

        txtField.setText("This is a long text. this is  long text.");
        assertTrue(txtField.getWidth() < TextInputSkinShim.getTextNode(txtField).getLayoutBounds().getWidth());
        assertEquals(0, TextInputSkinShim.getTextTranslateX(txtField), 0.0);
    }

    @Test
    public void testTextFieldCenterAlignment() {
        initStage();
        txtField.setSkin(new TextFieldSkin(txtField));
        txtField.setText("A short text.");
        txtField.setPrefColumnCount(20);
        txtField.setAlignment(Pos.BASELINE_CENTER);

        root.getChildren().add(txtField);
        stage.show();

        assertTrue(txtField.getWidth() > TextInputSkinShim.getTextNode(txtField).getLayoutBounds().getWidth());
        assertTrue(TextInputSkinShim.getTextTranslateX(txtField) > 0);

        txtField.setText("This is a long text. this is  long text.");
        assertTrue(txtField.getWidth() < TextInputSkinShim.getTextNode(txtField).getLayoutBounds().getWidth());
        assertEquals(0, TextInputSkinShim.getTextTranslateX(txtField), 0.0);
    }

    @Test public void stripInvalidCharacters() {
        txtField.setText("abcdefghijklm");
        char[] c = new char[]{0x7F, 0xA, 0x9, 0x00, 0x05, 0x10, 0x19};
        txtField.setText(String.valueOf(c));
        assertEquals("", txtField.getText());
    }

    //Test for JDK-8273657
    @Test
    public void testTextSelectionOnAddingTextField() {
        initStage();
        txtField.setSkin(new TextFieldSkin(txtField));
        txtField.setText("A short text");
        stage.show();

        root.getChildren().add(txtField);
        txtField.requestFocus();

        assertEquals(0, txtField.getSelection().getStart());
        assertEquals(txtField.getText().length(), txtField.getSelection().getEnd());

        root.getChildren().remove(txtField);
        root.getChildren().add(txtField);
        txtField.requestFocus();

        assertEquals(0, txtField.getSelection().getStart());
        assertEquals(txtField.getText().length(), txtField.getSelection().getEnd());
    }

    private Change upperCase(Change change) {
        change.setText(change.getText().toUpperCase());
        return change;
    }

    private Change noDigits(Change change) {
        Change filtered = change.clone();
        filtered.setText(change.getText().replaceAll("[0-9]","\n"));
        return filtered;
    }

    /**
     * Helper method to init the stage only if really needed.
     */
    private void initStage() {
        //This step is not needed (Just to make sure StubToolkit is loaded into VM)
        Toolkit tk = Toolkit.getToolkit();
        root = new StackPane();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
    }

    @AfterEach
    public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
        removeUncaughtExceptionHandler();
    }
}
