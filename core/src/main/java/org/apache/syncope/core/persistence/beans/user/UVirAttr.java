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
package org.apache.syncope.core.persistence.beans.user;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;

@Entity
public class UVirAttr extends AbstractVirAttr {

    private static final long serialVersionUID = 2943450934283989741L;

    @ManyToOne
    private SyncopeUser owner;

    @ManyToOne(fetch = FetchType.EAGER)
    private UVirSchema virtualSchema;

    @Override
    public <T extends AbstractAttributable> T getOwner() {
        return (T) owner;
    }

    @Override
    public <T extends AbstractAttributable> void setOwner(final T owner) {
        if (!(owner instanceof SyncopeUser)) {
            throw new ClassCastException("expected type SyncopeUser, found: " + owner.getClass().getName());
        }
        this.owner = (SyncopeUser) owner;
    }

    @Override
    public <T extends AbstractVirSchema> T getVirtualSchema() {
        return (T) virtualSchema;
    }

    @Override
    public <T extends AbstractVirSchema> void setVirtualSchema(final T virtualSchema) {
        if (!(virtualSchema instanceof UVirSchema)) {
            throw new ClassCastException("expected type UVirSchema, found: " + virtualSchema.getClass().getName());
        }
        this.virtualSchema = (UVirSchema) virtualSchema;
    }

    @Override
    public List<String> getValues() {
        if (values == null) {
            values = new ArrayList<String>();
        }
        return values;
    }
}
