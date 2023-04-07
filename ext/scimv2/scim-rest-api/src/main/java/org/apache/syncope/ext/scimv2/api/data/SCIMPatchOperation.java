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
package org.apache.syncope.ext.scimv2.api.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.List;
import org.apache.syncope.ext.scimv2.api.type.PatchOp;

@JsonDeserialize(using = SCIMPatchOperationDeserializer.class)
public class SCIMPatchOperation extends SCIMBean {

    private static final long serialVersionUID = 1802654766651071823L;

    private PatchOp op;

    private SCIMPatchPath path;

    private List<Serializable> value;

    public PatchOp getOp() {
        return op;
    }

    public void setOp(final PatchOp op) {
        this.op = op;
    }

    public SCIMPatchPath getPath() {
        return path;
    }

    public void setPath(final SCIMPatchPath path) {
        this.path = path;
    }

    public List<Serializable> getValue() {
        return value;
    }

    public void setValue(final List<Serializable> value) {
        this.value = value;
    }
}
