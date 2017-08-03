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

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.api.entity.SAML2UserTemplate;
import org.apache.syncope.core.persistence.jpa.entity.resource.AbstractAnyTemplate;

@Entity
@Table(name = JPASAML2UserTemplate.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "idp_id" }))
public class JPASAML2UserTemplate extends AbstractAnyTemplate implements SAML2UserTemplate {

    private static final long serialVersionUID = -4575039890434426856L;

    public static final String TABLE = "SAML2UserTemplate";

    @ManyToOne
    private JPASAML2IdP idp;

    @Override
    public SAML2IdP getIdP() {
        return idp;
    }

    @Override
    public void setIdP(final SAML2IdP idp) {
        checkType(idp, JPASAML2IdP.class);
        this.idp = (JPASAML2IdP) idp;
    }

}
