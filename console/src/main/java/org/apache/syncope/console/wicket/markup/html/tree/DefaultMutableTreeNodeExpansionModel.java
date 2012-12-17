/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.wicket.markup.html.tree;

import java.util.Set;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class DefaultMutableTreeNodeExpansionModel
        extends AbstractReadOnlyModel<Set<DefaultMutableTreeNode>> {

    private static final long serialVersionUID = -3407581132184748054L;

    @Override
    public Set<DefaultMutableTreeNode> getObject() {
        return DefaultMutableTreeNodeExpansion.get();
    }
}
