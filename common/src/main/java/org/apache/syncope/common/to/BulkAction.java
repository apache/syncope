/*
 * Copyright 2013 The Apache Software Foundation.
 *
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
package org.apache.syncope.common.to;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.AbstractBaseBean;

@XmlRootElement(name = "bulkAction")
@XmlType
public class BulkAction extends AbstractBaseBean {

    @XmlEnum
    @XmlType(name = "bulkActionType")
    public enum Type {

        DELETE,
        REACTIVATE,
        SUSPEND

    }

    private Type operation;

    /**
     * Serialized identifiers.
     */
    private Collection<String> targets;

    public Type getOperation() {
        return operation;
    }

    public void setOperation(final Type operation) {
        this.operation = operation;
    }

    public void setTargets(final Collection<String> targets) {
        this.targets = targets;
    }

    public Collection<String> getTargets() {
        return targets;
    }

    public void addTarget(final String target) {
        if (this.targets == null) {
            this.targets = new ArrayList<String>();
        }

        this.targets.add(target);
    }

    public int size() {
        return targets == null ? 0 : targets.size();
    }
}
