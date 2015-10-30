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
package org.apache.syncope.common.lib.to;

import org.apache.syncope.common.lib.AbstractBaseBean;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;

/**
 * Single propagation status.
 */
@XmlRootElement(name = "propagationStatus")
@XmlType
public class PropagationStatus extends AbstractBaseBean {

    private static final long serialVersionUID = 3921498450222857690L;

    /**
     * Object before propagation.
     */
    private ConnObjectTO beforeObj;

    /**
     * Object after propagation.
     */
    private ConnObjectTO afterObj;

    /**
     * Propagated resource name.
     */
    private String resource;

    /**
     * Propagation task execution status.
     */
    private PropagationTaskExecStatus status;

    /**
     * Propagation task execution failure message.
     */
    private String failureReason;

    public ConnObjectTO getAfterObj() {
        return afterObj;
    }

    public void setAfterObj(final ConnObjectTO afterObj) {
        this.afterObj = afterObj;
    }

    public ConnObjectTO getBeforeObj() {
        return beforeObj;
    }

    public void setBeforeObj(final ConnObjectTO beforeObj) {
        this.beforeObj = beforeObj;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public PropagationTaskExecStatus getStatus() {
        return status;
    }

    public void setStatus(final PropagationTaskExecStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }
}
