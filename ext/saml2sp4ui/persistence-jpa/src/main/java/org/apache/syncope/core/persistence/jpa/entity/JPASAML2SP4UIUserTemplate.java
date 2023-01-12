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
package org.apache.syncope.core.persistence.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIUserTemplate;

@Entity
@Table(name = JPASAML2SP4UIUserTemplate.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "idp_id" }))
public class JPASAML2SP4UIUserTemplate extends AbstractAnyTemplate implements SAML2SP4UIUserTemplate {

    private static final long serialVersionUID = -4575039890434426856L;

    public static final String TABLE = "SAML2SP4UIUserTemplate";

    @ManyToOne
    private JPASAML2SP4UIIdP idp;

    @Override
    public SAML2SP4UIIdP getIdP() {
        return idp;
    }

    @Override
    public void setIdP(final SAML2SP4UIIdP idp) {
        checkType(idp, JPASAML2SP4UIIdP.class);
        this.idp = (JPASAML2SP4UIIdP) idp;
    }
}
