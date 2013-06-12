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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.syncope.common.to.RoleTO;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;

public class DefaultMutableTreeNodeExpansion implements Set<DefaultMutableTreeNode>, Serializable {

    private static final long serialVersionUID = -2864060875425661224L;

    private static MetaDataKey<DefaultMutableTreeNodeExpansion> KEY =
            new MetaDataKey<DefaultMutableTreeNodeExpansion>() {

        private static final long serialVersionUID = 3109256773218160485L;

    };

    private Set<Long> ids = new HashSet<Long>();

    private boolean inverse;

    public void expandAll() {
        ids.clear();

        inverse = true;
    }

    public void collapseAll() {
        ids.clear();

        inverse = false;
    }

    @Override
    public boolean add(final DefaultMutableTreeNode node) {
        RoleTO roleTO = (RoleTO) node.getUserObject();
        boolean isAdded;
        if (inverse) {
            isAdded = ids.remove(roleTO.getId());
        } else {
            isAdded = ids.add(roleTO.getId());
        }
        return isAdded;
    }

    @Override
    public boolean remove(final Object object) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
        RoleTO roleTO = (RoleTO) node.getUserObject();
        boolean isRemoved;
        if (inverse) {
            isRemoved = ids.add(roleTO.getId());
        } else {
            isRemoved = ids.remove(roleTO.getId());
        }
        return isRemoved;
    }

    @Override
    public boolean contains(final Object object) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
        RoleTO roleTO = (RoleTO) node.getUserObject();
        boolean isContained;
        if (inverse) {
            isContained = !ids.contains(roleTO.getId());
        } else {
            isContained = ids.contains(roleTO.getId());
        }
        return isContained;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> A[] toArray(final A[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<DefaultMutableTreeNode> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends DefaultMutableTreeNode> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the expansion for the session.
     *
     * @return expansion
     */
    public static DefaultMutableTreeNodeExpansion get() {
        DefaultMutableTreeNodeExpansion expansion = Session.get().getMetaData(KEY);
        if (expansion == null) {
            expansion = new DefaultMutableTreeNodeExpansion();

            Session.get().setMetaData(KEY, expansion);
        }
        return expansion;
    }
}
