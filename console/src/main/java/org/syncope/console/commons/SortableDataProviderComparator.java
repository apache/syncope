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

import java.io.Serializable;
import java.util.Comparator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class SortableDataProviderComparator<T> implements
        Comparator<T>, Serializable {

    protected final SortableDataProvider<T> provider;

    public SortableDataProviderComparator(
            final SortableDataProvider<T> provider) {

        this.provider = provider;
    }

    protected int compare(final IModel<Comparable> model1,
            IModel<Comparable> model2) {

        int result;

        if (model1.getObject() == null && model2.getObject() == null) {
            result = 0;
        } else if (model1.getObject() == null) {
            result = 1;
        } else if (model2.getObject() == null) {
            result = -1;
        } else {
            result = model1.getObject().compareTo(model2.getObject());
        }

        result = provider.getSort().isAscending() ? result : -result;

        return result;
    }

    @Override
    public int compare(final T o1, final T o2) {
        IModel<Comparable> model1 = new PropertyModel<Comparable>(
                o1, provider.getSort().getProperty());
        IModel<Comparable> model2 = new PropertyModel<Comparable>(
                o2, provider.getSort().getProperty());

        return compare(model1, model2);
    }
}
