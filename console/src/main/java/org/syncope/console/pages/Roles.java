/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import org.syncope.console.wicket.markup.html.tree.PropertyEditableColumn;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyTreeColumn;
import org.apache.wicket.extensions.markup.html.tree.table.TreeTable;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Alignment;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation.Unit;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.tree.AbstractTree;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.console.commons.RoleTreeBuilder;

/**
 * Roles WebPage.
 */
public class Roles extends BasePage {

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    private TreeTable tree;

    private ModalWindow createRoleWin = null;

    private static final int WIN_HEIGHT = 500;

    private static final int WIN_WIDTH = 700;

    private WebMarkupContainer container;

    /**
     * Response flag set by the Modal Window after the operation is completed.
     */
    private boolean operationResult = false;

    public Roles(final PageParameters parameters) {
        super(parameters);

        add(createRoleWin = new ModalWindow("createRoleWin"));

        createRoleWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        createRoleWin.setInitialHeight(WIN_HEIGHT);
        createRoleWin.setInitialWidth(WIN_WIDTH);
        createRoleWin.setPageMapName("create-role-modal");
        createRoleWin.setCookieName("create-role-modal");

        container = new WebMarkupContainer("container");

        IColumn[] columns = new IColumn[]{
            new PropertyTreeColumn(
            new ColumnLocation(Alignment.LEFT, 30, Unit.EM),
            getString("column1"),
            "userObject.displayName"),
            new PropertyEditableColumn(
            new ColumnLocation(Alignment.LEFT, 20, Unit.EM),
            getString("column2"),
            "userObject.empty",
            createRoleWin,
            Roles.this)};

        Form form = new Form("form");
        add(form);

        tree = new TreeTable("treeTable", roleTreeBuilder.build(), columns);

        form.add(tree);
        tree.getTreeState().expandAll();
        tree.updateTree();

        container.add(tree);
        container.setOutputMarkupId(true);

        form.add(container);

        createRoleWin.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.addComponent(container);

                        if (operationResult) {
                            getSession().info(getString("operation_succeded"));
                        }

                        setResponsePage(Roles.class);
                    }
                });
    }

    protected AbstractTree getTree() {
        return tree;
    }

    public boolean isOperationResult() {
        return operationResult;
    }

    public void setOperationResult(boolean operationResult) {
        this.operationResult = operationResult;
    }
}
