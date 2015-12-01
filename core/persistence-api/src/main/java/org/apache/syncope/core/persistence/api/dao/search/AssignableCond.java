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
package org.apache.syncope.core.persistence.api.dao.search;

public class AssignableCond extends AbstractSearchCond {

    private static final long serialVersionUID = 1237627275756159522L;

    private String realmFullPath;

    /**
     * Whether this condition should be evaluated from the assignable group (default) - or instead the
     * assignee - point of view.
     * The converter from FIQL will ignore this setting, which is meant for internal usage.
     */
    private boolean fromGroup = true;

    public String getRealmFullPath() {
        return realmFullPath;
    }

    public void setRealmFullPath(final String realmFullPath) {
        this.realmFullPath = realmFullPath;
    }

    public boolean isFromGroup() {
        return fromGroup;
    }

    public void setFromGroup(final boolean fromGroup) {
        this.fromGroup = fromGroup;
    }

    @Override
    public final boolean isValid() {
        return realmFullPath != null;
    }
}
