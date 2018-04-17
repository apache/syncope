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

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.syncope.common.lib.AbstractBaseBean;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Reconciliation status.
 */
@XmlRootElement(name = "reconciliationStatus")
@XmlType
public class ReconStatus extends AbstractBaseBean {

    private static final long serialVersionUID = -8516345256596521490L;

    private String resource;

    private ConnObjectTO onSyncope;

    private ConnObjectTO onResource;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public ConnObjectTO getOnSyncope() {
        return onSyncope;
    }

    public void setOnSyncope(final ConnObjectTO onSyncope) {
        this.onSyncope = onSyncope;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public ConnObjectTO getOnResource() {
        return onResource;
    }

    public void setOnResource(final ConnObjectTO onResource) {
        this.onResource = onResource;
    }
}
