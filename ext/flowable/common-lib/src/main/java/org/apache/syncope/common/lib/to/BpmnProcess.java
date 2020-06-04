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

public class BpmnProcess implements EntityTO {

    private static final long serialVersionUID = -7044543391316529128L;

    private String key;

    private String modelId;

    private String name;

    private boolean userWorkflow;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(final String modelId) {
        this.modelId = modelId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isUserWorkflow() {
        return userWorkflow;
    }

    public void setUserWorkflow(final boolean userWorkflow) {
        this.userWorkflow = userWorkflow;
    }
}
