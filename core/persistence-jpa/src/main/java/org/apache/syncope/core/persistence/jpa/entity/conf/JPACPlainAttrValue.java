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
package org.apache.syncope.core.persistence.jpa.entity.conf;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;

@Entity
@Table(name = JPACPlainAttrValue.TABLE)
public class JPACPlainAttrValue extends AbstractPlainAttrValue implements CPlainAttrValue {

    private static final long serialVersionUID = -4029895248193486171L;

    public static final String TABLE = "CPlainAttrValue";

    @ManyToOne
    @NotNull
    private JPACPlainAttr attribute;

    @Override
    public CPlainAttr getAttr() {
        return attribute;
    }

    @Override
    public void setAttr(final PlainAttr<?> attr) {
        checkType(attr, JPACPlainAttr.class);
        this.attribute = (JPACPlainAttr) attr;
    }
}
