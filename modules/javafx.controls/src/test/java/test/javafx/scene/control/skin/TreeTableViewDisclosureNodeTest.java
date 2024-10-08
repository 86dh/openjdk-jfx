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

package test.javafx.scene.control.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils.getCell;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;

/**
 * Tests around disclosure node.
 */
public class TreeTableViewDisclosureNodeTest {

//------------ fields

    private Scene scene;
    private Stage stage;
    private Pane content;

    private TreeTableView<String> treeTable;
    private TreeItem<String> root;
    private TreeTableView.TreeTableViewSelectionModel<?> sm;

    // indices of root children
    private int rootLeafChildIndex;
    private int rootExpandedChildIndex;
    private int rootCollapsedChildIndex;

//------------

    /**
     * Test fix for JDK-8253597.
     */
    @Test
    public void testSelectChildLeafAfterExpand() {
        showTreeTable();
        TreeItem<String> child = root.getChildren().get(rootCollapsedChildIndex);
        // expand child that was initially collapsed
        child.setExpanded(true);
        Toolkit.getToolkit().firePulse();

        TreeItem<String> grandChild = child.getChildren().get(0);
        int grandChildRowIndex = treeTable.getRow(grandChild);
        assertTrue(grandChild.isLeaf(), "sanity: grandChild is leaf");
        assertFalse(sm.isSelected(grandChildRowIndex), "sanity: grandChild not selected");
        fireMouseIntoIndentationRegion(grandChildRowIndex);
        assertTrue(sm.isSelected(grandChildRowIndex), "grandChild must be selected " + grandChildRowIndex);
    }

    /**
     * This is the deeper reason for JDK-8253597: on re-use of a treeTableRow
     * in leaf rows, the disclosureNode is not removed
     */
    @Test
    @Disabled("real-cleanup")
    public void testRowReuse() {
        showTreeTable();
        TreeItem<String> expandedChild = root.getChildren().get(rootExpandedChildIndex);
        TreeItem<String> grandChild = expandedChild.getChildren().get(0);
        int grandChildRowIndex = treeTable.getRow(grandChild);
        assertNull(getDisclosureNode(grandChildRowIndex), "leaf must not have disclosureNode");
        // collapse/expand cycle
        expandedChild.setExpanded(false);
        Toolkit.getToolkit().firePulse();
        expandedChild.setExpanded(true);
        Toolkit.getToolkit().firePulse();
        assertNull(getDisclosureNode(grandChildRowIndex), "leaf must not have disclosureNode");
    }

    /**
     * Sanity: firing into disclosure node region of initially visible child leaf selects.
     */
    @Test
    public void testSelectChildLeaf() {
        showTreeTable();
        TreeItem<String> expandedChild = root.getChildren().get(rootExpandedChildIndex);
        TreeItem<String> grandChild = expandedChild.getChildren().get(0);
        int grandChildRowIndex = treeTable.getRow(grandChild);
        fireMouseIntoIndentationRegion(grandChildRowIndex);
        assertTrue(sm.isSelected(grandChildRowIndex), "row must be selected" + grandChildRowIndex);
    }

    /**
     * Sanity: firing into disclosure node region top-level leaf child selects.
     */
    @Test
    public void testSelectRootLeaf() {
        showTreeTable();
        TreeItem<String> leafChild = root.getChildren().get(rootLeafChildIndex);
        int leafChildRowIndex = treeTable.getRow(leafChild);
        fireMouseIntoIndentationRegion(leafChildRowIndex);
        assertTrue(sm.isSelected(leafChildRowIndex), "row must be selected" + leafChildRowIndex);
   }

    /**
     * Sanity test: firing into disclosure node region of collapsed child must expand it.
     */
    @Test
    public void testExpandCollapsedChild() {
        showTreeTable();
        TreeItem<String> child = root.getChildren().get(rootCollapsedChildIndex);
        boolean expanded = child.isExpanded();
        fireMouseIntoIndentationRegion(treeTable.getRow(child));
        assertEquals(!expanded,  child.isExpanded(), "expansion state changed" + child.getValue());
    }

    /**
     * Test inital state of disclosureNode.
     * Note that a leaf row that was visible initially, does not have
     * a disclosureNode.
     */
    @Test
    public void testInitialRowState() {
        showTreeTable();
        // root
        assertHasVisibleDisclosureNode(0);
        // leaf child of root
        assertNull(getDisclosureNode(rootLeafChildIndex + 1));
        // expanded child of root
        assertHasVisibleDisclosureNode(rootExpandedChildIndex + 1);
        assertNull(getDisclosureNode(rootExpandedChildIndex + 2));
    }

    /**
     * Fires a mouse pressed/released into the indentation region of the
     * first (single) tableCell of the given row.
     */
    protected void fireMouseIntoIndentationRegion(int rowIndex) {
        TreeTableRow<?> grandChildTableRow = getTableRow(rowIndex);
        // single column == single child cell
        TreeTableCell<?, ?> cell = (TreeTableCell<?, ?>) grandChildTableRow.lookup(".tree-table-cell");
        MouseEventFirer mouse = new MouseEventFirer(cell, true);
        // target to hit disclosure
        double targetX = - cell.getWidth() / 2; // compensate default center offset to zero
        mouse.fireMousePressAndRelease(1, targetX, 0);
        Toolkit.getToolkit().firePulse();
    }

    /**
     * Asserts that the tableRow at the given row has a disclosureNode with the
     * given visibility.
     *
     */
    protected void assertHasVisibleDisclosureNode(int rowIndex) {
        Node disclosure = getDisclosureNode(rowIndex);
        assertNotNull(disclosure, "disclosureNode must added");
        assertTrue(disclosure.isVisible(), "disclosureNode must be visible");
    }

// ------ accessor helpers

    /**
     * Returns the disclosureNode for the given rowIndex.
     */
    protected Node getDisclosureNode(int rowIndex) {
        TreeTableRow<?> tableRow = getTableRow(rowIndex);
        Node disclosure = tableRow.lookup(".tree-disclosure-node");
        return disclosure;
    }

    /**
     * Returns the TreeTableRow for the given treeItem. The item must be
     * accessible as specified by treeTable.getRow(treeItem).
     */
    protected TreeTableRow<?> getTableRow(TreeItem<String> treeItem) {
        return getTableRow(treeTable.getRow(treeItem));
    }

    /**
     * Returns the TreeTableRow for the given rowIndex. The index must
     * be in the range 0 <= rowIndex < treeTable.getExpandedItemCount()
     */
    protected TreeTableRow<?> getTableRow(int rowIndex) {
        IndexedCell<?> tableRow = getCell(treeTable, rowIndex);
        assertTrue(tableRow instanceof TreeTableRow, "sanity: expect TreeTableRow but was: " + tableRow);
        assertEquals(rowIndex, tableRow.getIndex(), "sanity: row index");
        return (TreeTableRow<?>) tableRow;
    }

  //---------------- setup and initial

    @Test
    public void testInitialTreeTableState() {
        assertTrue(treeTable.isShowRoot());
        assertSame(root, treeTable.getRoot());
        assertTrue(root.getChildren().get(rootLeafChildIndex).isLeaf());
        assertTrue(root.getChildren().get(rootExpandedChildIndex).isExpanded());
        assertFalse(root.getChildren().get(rootCollapsedChildIndex).isExpanded());
        int rowCount = root.getChildren().size() + 1 // root and direct children
                + root.getChildren().get(rootExpandedChildIndex).getChildren().size(); // expanded child,
        assertEquals(rowCount, treeTable.getExpandedItemCount());
        showTreeTable();
        List<Node> children = List.of(treeTable);
        assertEquals(children, content.getChildren());
        assertTrue(sm.isEmpty());
    }

    protected void showTreeTable() {
        showControl(treeTable);
    }

    /**
     * Ensures the control is shown and focused in an active scenegraph.
     */
    protected void showControl(Control control) {
        if (content == null) {
            content = new VBox();
            scene = new Scene(content);
            stage = new Stage();
            stage.setScene(scene);
        }
        if (!content.getChildren().contains(control)) {
            content.getChildren().add(control);
        }
        stage.show();
        stage.requestFocus();
        control.requestFocus();
        assertTrue(control.isFocused());
        assertSame(control, scene.getFocusOwner());
    }

    /**
     Tree structure:

         -v expanded root
               leaf             // rootLeafChildIndex
            -v expanded child   // rootExpandedChildIndex
                 child leaf
                 child leaf
                 child leaf
            -> collapsed child  // rootCollapsedChildIndex
            -> collapsed child
           ...
     */
    protected void fillTree(TreeItem<String> rootItem) {
        rootItem.setExpanded(true);
        rootItem.getChildren().add(0, new TreeItem<>("leafChild"));
        for (int i = 0; i < 10; i++) {
            TreeItem<String> newChild = new TreeItem<>("child " + i);
            if (i == 0) newChild.setExpanded(true);
            rootItem.getChildren().add(newChild);
            for (int j = 0; j < 3; j++) {
                TreeItem<String> newChild2 = new TreeItem<>(i + " grandChild " + j);
                newChild.getChildren().add(newChild2);
            }
        }
    }

    @BeforeEach
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
        rootLeafChildIndex = 0;
        rootExpandedChildIndex = 1;
        rootCollapsedChildIndex = 2;
        root = new TreeItem<>("Root");
        treeTable = new TreeTableView<>(root);
        fillTree(root);

        sm = treeTable.getSelectionModel();

        TreeTableColumn<String, String> treeColumn = new TreeTableColumn<>("Col1");
        treeColumn.setPrefWidth(200);
        treeColumn.setCellValueFactory(call -> new ReadOnlyStringWrapper(call.getValue().getValue()));
        treeTable.getColumns().add(treeColumn);
    }

    @AfterEach
    public void tearDown() {
        if (stage != null) stage.hide();
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

}
