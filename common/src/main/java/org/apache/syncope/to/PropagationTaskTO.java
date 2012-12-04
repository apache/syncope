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
package org.apache.syncope.to;

import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.PropagationOperation;

@XmlType
public class PropagationTaskTO extends TaskTO {

    private static final long serialVersionUID = 386450127003321197L;

    private PropagationMode propagationMode;

    private PropagationOperation propagationOperation;

    private String accountId;

    private String oldAccountId;

    private String xmlAttributes;

    private String resource;

    private long user;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOldAccountId() {
        return oldAccountId;
    }

    public void setOldAccountId(String oldAccountId) {
        this.oldAccountId = oldAccountId;
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public PropagationOperation getPropagationOperation() {
        return propagationOperation;
    }

    public void setPropagationOperation(PropagationOperation propagationOperation) {

        this.propagationOperation = propagationOperation;
    }

    public String getXmlAttributes() {
        return xmlAttributes;
    }

    public void setXmlAttributes(String xmlAttributes) {
        this.xmlAttributes = xmlAttributes;
    }

    public long getUser() {
        return user;
    }

    public void setUser(long user) {
        this.user = user;
    }
}
