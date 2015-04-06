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
package org.apache.syncope.client.console.commons;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.common.lib.to.GroupTO;
import org.springframework.beans.factory.annotation.Autowired;

public class GroupTreeBuilder {

    @Autowired
    private GroupRestClient restClient;

    private final GroupTOComparator comparator = new GroupTOComparator();

    private List<GroupTO> allGroups;

    private List<GroupTO> getChildGroups(final long parentGroupId, final List<GroupTO> groups) {
        List<GroupTO> result = new ArrayList<>();
        for (GroupTO group : groups) {
            if (group.getParent() == parentGroupId) {
                result.add(group);
            }
        }

        Collections.sort(result, comparator);
        return result;
    }

    private void populateSubtree(final DefaultMutableTreeNode subRoot, final List<GroupTO> groups) {
        GroupTO group = (GroupTO) subRoot.getUserObject();

        DefaultMutableTreeNode child;
        for (GroupTO subGroupTO : getChildGroups(group.getKey(), groups)) {
            child = new DefaultMutableTreeNode(subGroupTO);
            subRoot.add(child);
            populateSubtree(child, groups);
        }
    }

    public List<GroupTO> getAllGroups() {
        return this.allGroups;
    }

    public TreeModel build() {
        this.allGroups = this.restClient.list();
        return build(this.allGroups);
    }

    public TreeModel build(final List<GroupTO> groups) {
        DefaultMutableTreeNode fakeroot = new DefaultMutableTreeNode(new FakeRootGroupTO());

        populateSubtree(fakeroot, groups);

        return new DefaultTreeModel(fakeroot);
    }

    public GroupTO findGroup(final long groupKey) {
        GroupTO found = null;
        if (getAllGroups() != null) {
            for (GroupTO groupTO : getAllGroups()) {
                if (groupTO.getKey() == groupKey) {
                    found = groupTO;
                }
            }
        }
        return found;
    }

    private static class GroupTOComparator implements Comparator<GroupTO>, Serializable {

        private static final long serialVersionUID = 7085057398406518811L;

        @Override
        public int compare(final GroupTO r1, final GroupTO r2) {
            if (r1.getKey() < r2.getKey()) {
                return -1;
            }
            if (r1.getKey() == r2.getKey()) {
                return 0;
            }

            return 1;
        }
    }

    private static class FakeRootGroupTO extends GroupTO {

        private static final long serialVersionUID = 4839183625773925488L;

        public FakeRootGroupTO() {
            super();

            setKey(0);
            setName("");
            setParent(-1);
        }

        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
