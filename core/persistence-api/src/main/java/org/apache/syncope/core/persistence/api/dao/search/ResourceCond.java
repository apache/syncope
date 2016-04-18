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

/**
 * Search condition to be applied when searching for associated resources.
 */
public class ResourceCond extends AbstractSearchCond {

    private static final long serialVersionUID = 466054166309460002L;

    private String resourceKey;

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(final String resourceKey) {
        this.resourceKey = resourceKey;
    }

    @Override
    public final boolean isValid() {
        return resourceKey != null;
    }
}
