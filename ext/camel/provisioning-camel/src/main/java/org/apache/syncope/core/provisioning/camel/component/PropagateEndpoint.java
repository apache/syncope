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
package org.apache.syncope.core.provisioning.camel.component;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.camel.producer.AbstractProducer;
import org.apache.syncope.core.provisioning.camel.producer.ConfirmPasswordResetProducer;
import org.apache.syncope.core.provisioning.camel.producer.CreateProducer;
import org.apache.syncope.core.provisioning.camel.producer.DeleteProducer;
import org.apache.syncope.core.provisioning.camel.producer.DeprovisionProducer;
import org.apache.syncope.core.provisioning.camel.producer.ProvisionProducer;
import org.apache.syncope.core.provisioning.camel.producer.StatusProducer;
import org.apache.syncope.core.provisioning.camel.producer.SuspendProducer;
import org.apache.syncope.core.provisioning.camel.producer.UpdateProducer;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;

@UriEndpoint(scheme = "propagate", title = "propagate", syntax = "propagate:propagateType", producerOnly = true)
public class PropagateEndpoint extends DefaultEndpoint {

    @UriPath
    @Metadata(required = true)
    private PropagateType propagateType;

    @UriParam
    private AnyTypeKind anyTypeKind;

    @UriParam
    private boolean pull;

    private PropagationManager propagationManager;

    private PropagationTaskExecutor taskExecutor;

    private UserDAO userDAO;

    private GroupDAO groupDAO;

    private AnyObjectDAO anyObjectDAO;

    private GroupDataBinder groupDataBinder;

    private UserWorkflowAdapter uwfAdapter;

    private String executor;
    
    public PropagateEndpoint(final String endpointUri, final Component component) {
        super(endpointUri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        AbstractProducer producer = null;
        if (null != propagateType) {
            switch (propagateType) {
                case create:
                    producer = new CreateProducer(this, anyTypeKind);
                    break;
                case update:
                    producer = new UpdateProducer(this, anyTypeKind);
                    break;
                case delete:
                    producer = new DeleteProducer(this, anyTypeKind, userDAO, groupDataBinder);
                    break;
                case provision:
                    producer = new ProvisionProducer(this, anyTypeKind, userDAO);
                    break;
                case deprovision:
                    producer = new DeprovisionProducer(this, anyTypeKind, userDAO, groupDAO, anyObjectDAO);
                    break;
                case status:
                    producer = new StatusProducer(this, anyTypeKind, userDAO, uwfAdapter);
                    break;
                case suspend:
                    producer = new SuspendProducer(this, anyTypeKind);
                    break;
                case confirmPasswordReset:
                    producer = new ConfirmPasswordResetProducer(this, anyTypeKind);
                    break;
                default:
                    break;
            }
        }

        if (producer != null) {
            producer.setPropagationManager(propagationManager);
            producer.setPropagationTaskExecutor(taskExecutor);
            producer.setPull(pull);
            producer.setExecutor(this.executor);
        }
        return producer;
    }

    @Override
    public Consumer createConsumer(final Processor processor) throws Exception {
        throw new UnsupportedOperationException("Consumer not supported");
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public PropagateType getPropagateType() {
        return propagateType;
    }

    public void setPropagateType(final PropagateType propagateType) {
        this.propagateType = propagateType;
    }

    public void setPropagationManager(final PropagationManager propagationManager) {
        this.propagationManager = propagationManager;
    }

    public void setPropagationTaskExecutor(final PropagationTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    public void setUserDAO(final UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setGroupDAO(final GroupDAO groupDAO) {
        this.groupDAO = groupDAO;
    }

    public void setAnyObjectDAO(final AnyObjectDAO anyObjectDAO) {
        this.anyObjectDAO = anyObjectDAO;
    }

    public void setGroupDataBinder(final GroupDataBinder groupDataBinder) {
        this.groupDataBinder = groupDataBinder;
    }

    public boolean isPull() {
        return pull;
    }

    public void setPull(final boolean pull) {
        this.pull = pull;
    }

    public void setUwfAdapter(final UserWorkflowAdapter uwfAdapter) {
        this.uwfAdapter = uwfAdapter;
    }

    public void setExecutor(final String executor) {
        this.executor = executor;
    }
}
