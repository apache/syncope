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

import org.syncope.console.wicket.markup.html.tree.TreeModelBean;
import org.syncope.console.wicket.markup.html.tree.PropertyEditableColumn;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.PageParameters;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
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
import org.syncope.client.to.RoleTO;
import org.syncope.console.rest.RolesRestClient;

/**
 * Roles WebPage.
 */
public class Roles extends BasePage {

    @SpringBean(name = "rolesRestClient")
    RolesRestClient restClient;

    private TreeTable tree;

    List<Long> parentsId;

    WebMarkupContainer usersContainer;

    public Roles(PageParameters parameters) {
        super(parameters);

        IColumn columns[] = new IColumn[]{
        new PropertyTreeColumn(new ColumnLocation(Alignment.LEFT, 20,
            Unit.EM), getString("column1"), "userObject.treeNode.name"),
            new PropertyEditableColumn(new ColumnLocation(Alignment.LEFT, 20,
            Unit.EM), getString("column2"), "userObject.title"),
//            new PropertyRenderableColumn(new ColumnLocation(Alignment.MIDDLE, 3,
//                        Unit.PROPORTIONAL), "Name", "userObject.treeNode.name")
            };

        Form form = new Form("form");
        add(form);

        tree = new TreeTable("treeTable", createTreeModel(), columns);
        
        form.add(tree);
        tree.getTreeState().expandAll();
        tree.updateTree();

        usersContainer = new WebMarkupContainer("container");
        usersContainer.add(tree);
        usersContainer.setOutputMarkupId(true);


        form.add(usersContainer);
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     * @param window
     * @param container
     */
    public void setWindowClosedCallback(ModalWindow window,
            final WebMarkupContainer container) {

        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(container);
                    }
                });
    }

    /**
     * @see BaseTreePage#getTree()
     */
    protected AbstractTree getTree() {
        return tree;
    }

    /**
     * Creates the model that feeds the tree.
     *
     * @return New instance of tree model.
     */
    private TreeModel createTreeModel() {

        List<RoleTO> roles = restClient.getAllRoles().getRoles();
        
        List<TreeNode> roleTree = new ArrayList<TreeNode>();
        List<TreeNode> parentNodes = new ArrayList<TreeNode>();
        List<TreeNode> childNodes = new ArrayList<TreeNode>();

        parentsId = new ArrayList<Long>();

        //populate id list of parents
        for(RoleTO role : roles) {
                if(!parentExists(role.getParent(), parentsId))
                    parentsId.add(role.getParent());
        }

        //populate parents nodes list (parent = TreeNode)
        TreeNode parentNode;
        for(RoleTO role : roles) {
            for(Long parentId: parentsId)
                if(role.getId() == parentId){
                    parentNode = new TreeNode();
                    parentNode.setId(role.getId());
                    parentNode.setParentId(role.getParent());
                    parentNode.setName(role.getName());
                    parentNodes.add(parentNode);
                }
        }

        //populate children nodes list (parent = TreeNode)
        TreeNode childNode;
        for(RoleTO role : roles)
            if(!parentExists(role.getId(), parentsId)){
                childNode = new TreeNode();
                childNode.setId(role.getId());
                childNode.setName(role.getName());
                childNode.setParentId(role.getParent());
                childNodes.add(childNode);
            }

        //add leaf-nodes into child nodes list
        for(TreeNode node: childNodes) {
            addChildToParent(parentNodes,node);
        }

        //nest parents each-others
        for(TreeNode node : parentNodes) {
            addChildToParent(roleTree,node);
        }
        
        return convertToTreeModel(roleTree);
    }

    /**
     * Check if a parent has already been inserted in the parents id list
     * @param parentId
     * @param list
     * @return true if a parent id is already in the list, false otherwise
     */
    public boolean parentExists(Long parentId,List<Long>list){
        boolean found = false;

        for(Long id : list){
            if(!found && id == parentId)
                found = true;
        }
        return found;
    }

    /**
     * Add a node child to the specific tree
     * @param nodes list populate
     * @param child to be added to list
     * @return List<TreeNode>
     */
    public List<TreeNode> addChildToParent(List<TreeNode> nodes,TreeNode child){
        if(nodes.size() == 0){
            nodes.add(child);
            return nodes;
        }
        //if child is the root it won't be a child, so just ignore it
        if(child.getParentId() == child.getId())
            return nodes;

        else {
            for(TreeNode item : nodes) {
                if(item.getId() == child.getParentId()){
                    item.getChildren().add(child);
                    break;
                }
            }
        }

        return nodes;
    }

    private TreeModel convertToTreeModel(List<TreeNode> list) {
        TreeModel model = null;
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new TreeModelBean("Root"));
        add(rootNode, list);
        model = new DefaultTreeModel(rootNode);
        return model;
    }

    private void add(DefaultMutableTreeNode parent, List<TreeNode> sub) {
        for (Iterator<TreeNode> i = sub.iterator(); i.hasNext();) {
            TreeNode node = i.next();
            if (node.getChildren().size() > 0) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode
                    (new TreeModelBean(node));
                parent.add(child);
                add(child, node.getChildren());
            } else {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode
                    (new TreeModelBean(node));
                parent.add(child);
            }
        }
    }

    /**
     * TreeNode wrapper class for RoleTO object.
     */
    public class TreeNode {
        Long id;

        Long parentId;

        String name;

        List<TreeNode> children = new ArrayList<TreeNode>();

        public List<TreeNode> getChildren() {
            return children;
        }

        public void setChildren(List<TreeNode> children) {
            this.children = children;
        }

        public Long getId() {
            return id;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }
        
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}