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

import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.types.PropagationTaskExecStatus;

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
    private ConnObjectTO beforeObj;

    /**
     * Object after propagation.
     */
    private ConnObjectTO afterObj;

    /**
     * Propagated resource name.
     */
    private String resourceName;

    /**
     * Propagation task execution status.
     */
    private PropagationTaskExecStatus status;

    /**
     * After object getter.
     *
     * @return after object.
     */
    public ConnObjectTO getAfterObj() {
        return afterObj;
    }

    /**
     * After object setter.
     *
     * @param afterObj object.
     */
    public void setAfterObj(final ConnObjectTO afterObj) {
        this.afterObj = afterObj;
    }

    /**
     * Before object getter.
     *
     * @return before object.
     */
    public ConnObjectTO getBeforeObj() {
        return beforeObj;
    }

    /**
     * Before object setter.
     *
     * @param beforeObj object.
     */
    public void setBeforeObj(final ConnObjectTO beforeObj) {
        this.beforeObj = beforeObj;
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
