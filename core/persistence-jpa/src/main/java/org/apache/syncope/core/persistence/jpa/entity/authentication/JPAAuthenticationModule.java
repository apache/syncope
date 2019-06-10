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
package org.apache.syncope.core.persistence.jpa.entity.authentication;


import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.AMImplementationType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationModule;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@Entity
@Table(name = JPAAuthenticationModule.TABLE)
public class JPAAuthenticationModule extends AbstractGeneratedKeyEntity implements AuthenticationModule {

    private static final long serialVersionUID = 7422422526695279794L;

    public static final String TABLE = "AuthenticationModule";

    @Column(unique = true, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TraceLevel traceLevel = TraceLevel.FAILURES;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Conf",
            joinColumns =
            @JoinColumn(name = "authentication_module_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"))
    private List<JPAImplementation> configurations = new ArrayList<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public TraceLevel getTraceLevel() {
        return traceLevel;
    }

    @Override
    public List<? extends Implementation> getConfigurations() {
        return configurations;
    }

    @Override
    public void setTraceLevel(final TraceLevel createTraceLevel) {
        this.traceLevel = createTraceLevel;
    }

    @Override
    public boolean add(final Implementation configuration) {
        checkType(configuration, JPAImplementation.class);
        checkImplementationType(configuration, AMImplementationType.AUTH_MODULE_CONFIGURATIONS);
        return configurations.contains((JPAImplementation) configuration)
                || configurations.add((JPAImplementation) configuration);
    }

}
