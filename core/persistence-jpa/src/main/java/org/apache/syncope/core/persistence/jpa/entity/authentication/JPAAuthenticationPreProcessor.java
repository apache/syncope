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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import org.apache.syncope.common.lib.types.AMImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationPreProcessor;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@Entity
@Table(name = JPAAuthenticationPreProcessor.TABLE)
public class JPAAuthenticationPreProcessor 
        extends AbstractAuthenticationProcessor implements AuthenticationPreProcessor  {

    private static final long serialVersionUID = -3064505653663946579L;

    public static final String TABLE = "AuthenticationPreProcessor";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Actions",
            joinColumns =
            @JoinColumn(name = "authentication_pre_processor"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"))
    private List<JPAImplementation> authenticationPreProcessing = new ArrayList<>();

    @Override
    public List<? extends Implementation> getAuthenticationPreProcessing() {
        return authenticationPreProcessing;
    }

    @Override
    public boolean addAuthPreProcessing(final Implementation authPreProcessing) {
        checkType(authPreProcessing, JPAImplementation.class);
        checkImplementationType(authPreProcessing, AMImplementationType.AUTH_PRE_PROCESSING);
        return authenticationPreProcessing.contains((JPAImplementation) authPreProcessing)
                || authenticationPreProcessing.add((JPAImplementation) authPreProcessing);
    }

}
