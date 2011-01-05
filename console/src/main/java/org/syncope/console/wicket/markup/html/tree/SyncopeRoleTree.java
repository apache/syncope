/*
 *  Copyright 2010 luis.
 * 
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import org.springframework.web.client.RestClientException;
import org.syncope.client.to.RoleTO;
import org.syncope.console.rest.RoleRestClient;

/**
 *
 */
public class SyncopeRoleTree {
    List<Long> parentsId;
    RoleRestClient restClient;

    public SyncopeRoleTree(RoleRestClient restClient) {
        this.restClient = restClient;
    }


    /**
     * Creates the model that feeds the tree.
     *
     * @return New instance of tree model.
     */
    public TreeModel createTreeModel() throws RestClientException {

        List<RoleTO> roles = restClient.getAllRoles();

        List<SyncopeTreeNode> roleTree = new ArrayList<SyncopeTreeNode>();
        List<SyncopeTreeNode> parentNodes = new ArrayList<SyncopeTreeNode>();
        List<SyncopeTreeNode> childNodes = new ArrayList<SyncopeTreeNode>();

        parentsId = new ArrayList<Long>();

        //populate id list of parents
        for (RoleTO role : roles) {
            if (!parentExists(role.getParent(), parentsId)) {
                parentsId.add(role.getParent());
            }
        }

        //populate parents nodes list (parent = SyncopeTreeNode)
        SyncopeTreeNode parentNode;
        for (RoleTO role : roles) {
            for (Long parentId : parentsId) {
                if (role.getId() == parentId) {
                    parentNode = new SyncopeTreeNode();
                    parentNode.setId(role.getId());
                    parentNode.setParentId(role.getParent());
                    parentNode.setName(role.getName());
                    parentNodes.add(parentNode);
                }
            }
        }

        //populate children nodes list (parent = SyncopeTreeNode)
        SyncopeTreeNode childNode;
        for (RoleTO role : roles) {
            if (!parentExists(role.getId(), parentsId)) {
                childNode = new SyncopeTreeNode();
                childNode.setId(role.getId());
                childNode.setName(role.getName());
                childNode.setParentId(role.getParent());
                childNodes.add(childNode);
            }
        }

        //add leaf-nodes into child nodes list
        for (SyncopeTreeNode node : childNodes) {
            addChildToParent(parentNodes, node);
        }

        //nest parents each-others
        for (SyncopeTreeNode node : parentNodes) {
            addChildToParent(roleTree, node);
        }

        return convertToTreeModel(roleTree);
    }

    /**
     * Check if a parent has already been inserted in the parents id list
     * @param parentId
     * @param list
     * @return true if a parent id is already in the list, false otherwise
     */
    public boolean parentExists(Long parentId, List<Long> list) {
        boolean found = false;

        for (Long id : list) {
            if (!found && id.longValue() == parentId.longValue()) {
                found = true;
            }
        }
        return found;
    }

    /**
     * Add a node child to the specific parent
     * @param nodes list populate
     * @param child to be added to list
     * @return List<SyncopeTreeNode>
     */
    public List<SyncopeTreeNode> addChildToParent(List<SyncopeTreeNode> nodes,
            SyncopeTreeNode child) {
        if (nodes.size() == 0) {
            nodes.add(child);
            return nodes;
        }
        //if child is the root it won't be a child, so just ignore it
        if (child.getParentId().longValue() == child.getId().longValue()) {
            return nodes;
        } else {
            for (SyncopeTreeNode item : nodes) {
                if (item.getId().longValue() == child.getParentId()
                        .longValue()) {
                    item.getChildren().add(child);
                    break;
                } else if (item.getId().longValue() != child.getParentId()
                        .longValue() && item.getChildren().size() > 0) {
                    addChildToParent(item.getChildren(), child);
                }
            }
        }

        return nodes;
    }

    public TreeModel convertToTreeModel(List<SyncopeTreeNode> list) {
        TreeModel model = null;
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(
                new TreeModelBean("Root"));
        add(rootNode, list);
        model = new DefaultTreeModel(rootNode);
        return model;
    }

    public void add(DefaultMutableTreeNode parent, List<SyncopeTreeNode> sub) {
        for (Iterator<SyncopeTreeNode> i = sub.iterator(); i.hasNext();) {
            SyncopeTreeNode node = i.next();
            if (node.getChildren().size() > 0) {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(
                        new TreeModelBean(node));
                parent.add(child);
                add(child, node.getChildren());
            } else {
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(
                        new TreeModelBean(node));
                parent.add(child);
            }
        }
    }
}