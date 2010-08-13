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

import java.io.Serializable;

public class TreeModelBean implements Serializable
{
    private SyncopeTreeNode treeNode;
    private String title;

    public TreeModelBean(String title) {
        this.title = title;
    }
    
    public TreeModelBean(SyncopeTreeNode tree)
    {
        this.treeNode = tree;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public SyncopeTreeNode getTreeNode() {
        return treeNode;
    }

    public void setTreeNode(SyncopeTreeNode treeNode) {
        this.treeNode = treeNode;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        //return treeNode.getName();
        return "";
    }
}