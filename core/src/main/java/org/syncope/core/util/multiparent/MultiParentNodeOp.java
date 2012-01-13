/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.util.multiparent;

import java.util.List;

public class MultiParentNodeOp {

    private MultiParentNodeOp() {
    }

    public static <T> MultiParentNode<T> findInTree(
            final MultiParentNode<T> parent, final T object) {

        if (parent.getObject().equals(object)) {
            return parent;
        }

        for (MultiParentNode<T> child : parent.getChildren()) {
            MultiParentNode<T> found = findInTree(child, object);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    public static <T> void traverseTree(final MultiParentNode<T> parent,
            final List<T> objects) {

        for (MultiParentNode<T> child : parent.getChildren()) {
            traverseTree(child, objects);
        }

        if (!objects.contains(parent.getObject())) {
            objects.add(parent.getObject());
        }
    }
}
