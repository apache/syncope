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
package org.apache.syncope.core.persistence.common.content;

import java.util.Collection;
import java.util.Set;

public final class MultiParentNodeOp {

    public static void traverseTree(final Set<MultiParentNode> roots, final Collection<String> objects) {
        for (MultiParentNode root : roots) {
            traverseTree(root, objects);
        }
    }

    public static void traverseTree(final MultiParentNode root, final Collection<String> objects) {
        root.setExploited(true);

        for (MultiParentNode child : root.getChildren()) {
            if (!child.isExploited()) {
                traverseTree(child, objects);
            }
        }

        if (!objects.contains(root.getObject())) {
            objects.add(root.getObject());
        }
    }

    private MultiParentNodeOp() {
        // private constructor for static utility class
    }
}
