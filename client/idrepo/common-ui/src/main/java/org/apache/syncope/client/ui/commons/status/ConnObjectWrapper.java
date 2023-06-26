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
package org.apache.syncope.client.ui.commons.status;

import java.io.Serializable;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObject;

public class ConnObjectWrapper implements Serializable {

    private static final long serialVersionUID = 9083721948999924299L;

    private final AnyTO any;

    private final String resource;

    private final ConnObject connObjectTO;

    public ConnObjectWrapper(final AnyTO any, final String resource, final ConnObject connObjectTO) {
        this.any = any;
        this.resource = resource;
        this.connObjectTO = connObjectTO;
    }

    public AnyTO getAny() {
        return any;
    }

    public String getResource() {
        return resource;
    }

    public ConnObject getConnObjectTO() {
        return connObjectTO;
    }
}
