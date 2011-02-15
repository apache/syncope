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
package org.syncope.console.commons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.to.RoleTO;
import org.syncope.console.rest.RoleRestClient;

public class RoleTreeBuilder {

    @Autowired
    private RoleRestClient restClient;

    private RoleTOComparator comparator = new RoleTOComparator();

    private List<RoleTO> getChildRoles(final long parentRoleId,
            final List<RoleTO> roles) {

        List<RoleTO> result = new ArrayList<RoleTO>();
        for (RoleTO role : roles) {
            if (role.getParent() == parentRoleId) {
                result.add(role);
            }
        }

        Collections.sort(result, comparator);
        return result;
    }

    private void populateSubtree(final DefaultMutableTreeNode subRoot,
            final List<RoleTO> roles) {

        RoleTO role = (RoleTO) subRoot.getUserObject();

        DefaultMutableTreeNode child;
        for (RoleTO subRoleTO : getChildRoles(role.getId(), roles)) {
            child = new DefaultMutableTreeNode(subRoleTO);
            subRoot.add(child);
            populateSubtree(child, roles);
        }
    }

    public TreeModel build() {
        return build(restClient.getAllRoles());
    }

    public TreeModel build(final List<RoleTO> roles) {
        DefaultMutableTreeNode fakeroot =
                new DefaultMutableTreeNode(new FakerootTO());

        populateSubtree(fakeroot, roles);

        return new DefaultTreeModel(fakeroot);
    }

    private class RoleTOComparator implements Comparator<RoleTO> {

        @Override
        public int compare(final RoleTO r1, final RoleTO r2) {
            if (r1.getId() < r2.getId()) {
                return -1;
            }
            if (r1.getId() == r2.getId()) {
                return 0;
            }

            return 1;
        }
    }

    private static class FakerootTO extends RoleTO {

        public FakerootTO() {
            super();

            setId(0);
            setName("");
            setParent(-1);
        }

        @Override
        public String getDisplayName() {
            return "";
        }
    }
}
