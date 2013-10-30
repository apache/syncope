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
package org.apache.syncope.console.commons.status;

import java.io.Serializable;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.ConnObjectTO;

public class ConnObjectWrapper implements Serializable {

    private static final long serialVersionUID = 9083721948999924299L;

    private final AbstractAttributableTO attributable;

    private final String resourceName;

    private final ConnObjectTO connObjectTO;

    public ConnObjectWrapper(final AbstractAttributableTO attributable, final String resourceName,
            final ConnObjectTO connObjectTO) {

        this.attributable = attributable;
        this.resourceName = resourceName;
        this.connObjectTO = connObjectTO;
    }

    public AbstractAttributableTO getAttributable() {
        return attributable;
    }

    public String getResourceName() {
        return resourceName;
    }

    public ConnObjectTO getConnObjectTO() {
        return connObjectTO;
    }

}
