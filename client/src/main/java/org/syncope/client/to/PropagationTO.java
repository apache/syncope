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
package org.syncope.client.to;

import org.syncope.client.AbstractBaseBean;
import org.syncope.types.PropagationTaskExecStatus;

/**
 * Single propagation status.
 */
public class PropagationTO extends AbstractBaseBean {

    /**
     * Serial version ID.
     */
    private static final long serialVersionUID = 3921498450222857690L;

    /**
     * Object before propagation.
     */
    private ConnObjectTO before;

    /**
     * Object after propagation.
     */
    private ConnObjectTO after;

    /**
     * Propagated resource name.
     */
    private String resourceName;

    /**
     * Propagation task excution status.
     */
    private PropagationTaskExecStatus status;

    /**
     * After object getter.
     *
     * @return after object.
     */
    public ConnObjectTO getAfter() {
        return after;
    }

    /**
     * After object setter.
     *
     * @param after object.
     */
    public void setAfter(final ConnObjectTO after) {
        this.after = after;
    }

    /**
     * Before object getter.
     *
     * @return before object.
     */
    public ConnObjectTO getBefore() {
        return before;
    }

    /**
     * Before object setter.
     *
     * @param before object.
     */
    public void setBefore(final ConnObjectTO before) {
        this.before = before;
    }

    /**
     * resource name getter.
     *
     * @return resource name.
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Resource name setter.
     *
     * @param resourceName resource name.
     */
    public void setResourceName(final String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * Propagation execution status getter.
     *
     * @return status.
     */
    public PropagationTaskExecStatus getStatus() {
        return status;
    }

    /**
     * Propagation execution status setter.
     *
     * @param status propagation execution status.
     */
    public void setStatus(final PropagationTaskExecStatus status) {
        this.status = status;
    }
}
