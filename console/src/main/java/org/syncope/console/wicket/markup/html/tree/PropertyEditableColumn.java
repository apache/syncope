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
package org.syncope.console.wicket.markup.html.tree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.tree.table.ColumnLocation;
import org.apache.wicket.extensions.markup.html.tree.table.IColumn;
import org.apache.wicket.extensions.markup.html.tree.table.IRenderable;
import org.apache.wicket.extensions.markup.html.tree.table.PropertyRenderableColumn;
import org.apache.wicket.model.PropertyModel;

public class PropertyEditableColumn extends PropertyRenderableColumn {
ModalWindow window = null;
    /**
     * Column constructor.
     *
     * @param location
     * @param header
     * @param propertyExpression
     */
    public PropertyEditableColumn(ColumnLocation location, String header,
                                  String propertyExpression,ModalWindow window)
    {
        super(location, header, propertyExpression);
        
        this.window = window;
    }

    /**
     * @see IColumn#newCell(MarkupContainer, String, TreeNode, int)
     */
    @Override
    public Component newCell(MarkupContainer parent, String id, 
            TreeNode node, int level) {
        DefaultMutableTreeNode syncopeTreeNode = (DefaultMutableTreeNode) node;
        TreeModelBean treeModel = (TreeModelBean) syncopeTreeNode.getUserObject();

        NodeEditablePanel editablePanel;
        
        editablePanel = new NodeEditablePanel(id, treeModel.getTreeNode().getId(),
                    new PropertyModel(node, getPropertyExpression()),window);


        return editablePanel;
    }

    /**
     * @see IColumn#newCell(TreeNode, int)
     */
    @Override
    public IRenderable newCell(TreeNode node, int level) {
        if (getTreeTable().getTreeState().isNodeSelected(node)) {
            //getTreeTable().setVisibilityAllowed(true);
            return null;

        } else {
            //getTreeTable().setVisibilityAllowed(false);

            return super.newCell(node, level);
        }
    }
}
