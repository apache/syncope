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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.jpa.validation.entity.PrivilegeCheck;

@Entity
@Table(name = JPAPrivilege.TABLE)
@PrivilegeCheck
public class JPAPrivilege extends AbstractProvidedKeyEntity implements Privilege {

    private static final long serialVersionUID = -6479069294944858456L;

    public static final String TABLE = "Privilege";

    @ManyToOne
    private JPAApplication application;

    private String description;

    @Lob
    private String spec;

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public void setApplication(final Application application) {
        checkType(application, JPAApplication.class);
        this.application = (JPAApplication) application;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getSpec() {
        return spec;
    }

    @Override
    public void setSpec(final String spec) {
        this.spec = spec;
    }

}
