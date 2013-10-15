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
package org.apache.syncope.common.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.AbstractBaseBean;

@XmlRootElement(name = "bulkAssociationAction")
@XmlType
public class BulkAssociationAction extends AbstractBaseBean {

    private static final long serialVersionUID = 1395353278878758961L;

    @XmlEnum
    @XmlType(name = "bulkAssociationActionType")
    public enum Type {

        UNLINK,
        UNASSIGN,
        DEPROVISION

    }

    private Type operation;

    /**
     * Serialized identifiers.
     */
    private final List<Long> targets = new ArrayList<Long>();

    public Type getOperation() {
        return operation;
    }

    public void setOperation(final Type operation) {
        this.operation = operation;
    }

    @XmlElementWrapper(name = "targets")
    @XmlElement(name = "target")
    @JsonProperty("targets")
    public Collection<Long> getTargets() {
        return targets;
    }
}
