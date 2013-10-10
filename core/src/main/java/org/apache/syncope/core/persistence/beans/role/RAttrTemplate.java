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
package org.apache.syncope.core.persistence.beans.role;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.apache.syncope.core.persistence.beans.AbstractAttrTemplate;

@Entity
public class RAttrTemplate extends AbstractAttrTemplate<RSchema> {

    private static final long serialVersionUID = -3424574558427502145L;

    @ManyToOne
    private SyncopeRole owner;

    @ManyToOne
    @JoinColumn(name = "schema_name")
    private RSchema schema;

    @Override
    public RSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final RSchema schema) {
        this.schema = schema;
    }

    @Override
    public SyncopeRole getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final SyncopeRole owner) {
        this.owner = owner;
    }
}
