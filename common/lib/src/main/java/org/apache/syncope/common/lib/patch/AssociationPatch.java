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
package org.apache.syncope.common.lib.patch;

import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;

@XmlRootElement(name = "associationPatch")
@XmlType
public class AssociationPatch extends PasswordPatch {

    private static final long serialVersionUID = 6295778399633883767L;

    private String key;

    private ResourceAssociationAction action;

    public String getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public ResourceAssociationAction getAction() {
        return action;
    }

    @PathParam("action")
    public void setAction(final ResourceAssociationAction action) {
        this.action = action;
    }
}
