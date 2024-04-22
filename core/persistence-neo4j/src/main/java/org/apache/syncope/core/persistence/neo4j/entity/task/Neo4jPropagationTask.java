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
package org.apache.syncope.core.persistence.neo4j.entity.task;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.common.validation.PropagationTaskCheck;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Encapsulate all information about a propagation task.
 */
@Node(Neo4jPropagationTask.NODE)
@PropagationTaskCheck
public class Neo4jPropagationTask extends AbstractTask<PropagationTask> implements PropagationTask {

    private static final long serialVersionUID = 7086054884614511210L;

    public static final String NODE = "PropagationTask";

    public static final String PROPAGATION_TASK_EXEC_REL = "PROPAGATION_TASK_EXEC";

    /**
     * @see ResourceOperation
     */
    private ResourceOperation operation;

    /**
     * The connObjectKey on the external resource.
     */
    private String connObjectKey;

    /**
     * The (optional) former connObjectKey on the external resource.
     */
    private String oldConnObjectKey;

    /**
     * Data to be propagated.
     */
    private String propagationData;

    private String objectClassName;

    private AnyTypeKind anyTypeKind;

    private String anyType;

    private String entityKey;

    /**
     * ExternalResource to which the propagation happens.
     */
    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jExternalResource resource;

    @Relationship(type = PROPAGATION_TASK_EXEC_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jPropagationTaskExec> executions = new ArrayList<>();

    @Override
    public String getConnObjectKey() {
        return connObjectKey;
    }

    @Override
    public void setConnObjectKey(final String connObjectKey) {
        this.connObjectKey = connObjectKey;
    }

    @Override
    public String getOldConnObjectKey() {
        return oldConnObjectKey;
    }

    @Override
    public void setOldConnObjectKey(final String oldConnObjectKey) {
        this.oldConnObjectKey = oldConnObjectKey;
    }

    @Override
    public String getSerializedPropagationData() {
        return propagationData;
    }

    @Override
    public PropagationData getPropagationData() {
        PropagationData result = null;
        if (StringUtils.isNotBlank(propagationData)) {
            result = POJOHelper.deserialize(propagationData, PropagationData.class);
        }
        return result;
    }

    @Override
    public void setPropagationData(final PropagationData propagationData) {
        this.propagationData = POJOHelper.serialize(propagationData);
    }

    @Override
    public ResourceOperation getOperation() {
        return operation;
    }

    @Override

    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    @Override
    public String getObjectClassName() {
        return objectClassName;
    }

    @Override
    public void setObjectClassName(final String objectClassName) {
        this.objectClassName = objectClassName;
    }

    @Override
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    @Override
    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @Override
    public String getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final String anyType) {
        this.anyType = anyType;
    }

    @Override
    public String getEntityKey() {
        return entityKey;
    }

    @Override
    public void setEntityKey(final String entityKey) {
        this.entityKey = entityKey;
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, Neo4jExternalResource.class);
        this.resource = (Neo4jExternalResource) resource;
    }

    @Override
    protected boolean doAdd(final TaskExec<PropagationTask> exec) {
        return executions.add((Neo4jPropagationTaskExec) exec);
    }

    @Override
    protected Class<? extends AbstractTaskExec<PropagationTask>> executionClass() {
        return Neo4jPropagationTaskExec.class;
    }

    @Override
    protected List<? extends AbstractTaskExec<PropagationTask>> executions() {
        return executions;
    }
}
