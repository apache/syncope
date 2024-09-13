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
package org.apache.syncope.core.persistence.neo4j.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractPlainAttr;

public class JSONLAPlainAttr extends AbstractPlainAttr<User> implements LAPlainAttr {

    private static final long serialVersionUID = 7827533741035423694L;

    /**
     * The owner of this attribute.
     */
    @JsonIgnore
    private Neo4jUser owner;

    @JsonIgnore
    private Neo4jLinkedAccount account;

    /**
     * Values of this attribute (if schema is not UNIQUE).
     */
    private List<JSONLAPlainAttrValue> values = new ArrayList<>();

    /**
     * Value of this attribute (if schema is UNIQUE).
     */
    @JsonProperty
    private JSONLAPlainAttrUniqueValue uniqueValue;

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        this.owner = (Neo4jUser) owner;
    }

    @Override
    public LinkedAccount getAccount() {
        return account;
    }

    @Override
    public void setAccount(final LinkedAccount account) {
        this.account = (Neo4jLinkedAccount) account;
    }

    @Override
    protected boolean addForMultiValue(final PlainAttrValue attrValue) {
        return values.add((JSONLAPlainAttrValue) attrValue);
    }

    @Override
    public List<? extends LAPlainAttrValue> getValues() {
        return values;
    }

    @Override
    public LAPlainAttrUniqueValue getUniqueValue() {
        return uniqueValue;
    }

    @JsonIgnore
    @Override
    public void setUniqueValue(final PlainAttrUniqueValue uniqueValue) {
        this.uniqueValue = (JSONLAPlainAttrUniqueValue) uniqueValue;
    }
}
