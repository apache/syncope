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
import java.util.ArrayList;
import java.util.List;

/**
 * SyncopeTreeNode wrapper class for RoleTO object.
 */
public class SyncopeTreeNode implements Serializable {

    Long id;
    Long parentId;
    String name;
    List<SyncopeTreeNode> children = new ArrayList<SyncopeTreeNode>();

    public List<SyncopeTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<SyncopeTreeNode> children) {
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

    public String displayName(){
    return getId()+"-"+getName();
    }
}