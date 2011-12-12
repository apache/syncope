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
package org.syncope.types;

import java.util.ArrayList;
import java.util.List;
import org.syncope.client.AbstractBaseBean;

public class ConnConfProperty extends AbstractBaseBean
        implements Comparable<ConnConfProperty> {

    private static final long serialVersionUID = -8391413960221862238L;

    private ConnConfPropSchema schema;

    private List values;

    private boolean overridable;

    public ConnConfPropSchema getSchema() {
        return schema;
    }

    public void setSchema(ConnConfPropSchema schema) {
        this.schema = schema;
    }

    public List getValues() {
        if (values == null) {
            values = new ArrayList();
        }
        return values;
    }

    public void setValues(List values) {
        this.values = values;
    }

    public boolean isOverridable() {
        return overridable;
    }

    public void setOverridable(boolean overridable) {
        this.overridable = overridable;
    }

    @Override
    public int compareTo(final ConnConfProperty connConfProperty) {
        return this.getSchema().compareTo(connConfProperty.getSchema());
    }
}
