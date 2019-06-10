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
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.AMImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.authentication.AuthenticationPostProcessor;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;

@Entity
@Table(name = JPAAuthenticationPostProcessor.TABLE)
public class JPAAuthenticationPostProcessor 
        extends AbstractAuthenticationProcessor implements AuthenticationPostProcessor  {

    private static final long serialVersionUID = 8759966056325625080L;

    public static final String TABLE = "AuthenticationPostProcessor";

    @NotNull
    private String defaultSuccessLoginURL;

    @NotNull
    private String defaultFailureLoginURL;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Actions",
            joinColumns =
            @JoinColumn(name = "authentication_post_processor"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"))
    private List<JPAImplementation> authenticationPostProcessing = new ArrayList<>();

    @Override
    public String getDefaultSuccessLoginURL() {
        return defaultSuccessLoginURL;
    }

    @Override
    public String getDefaultFailureLoginURL() {
        return defaultFailureLoginURL;
    }

    @Override
    public List<? extends Implementation> getAuthenticationPostProcessing() {
        return authenticationPostProcessing;
    }

    @Override
    public void setDefaultSuccessLoginURL(final String defaultSuccessLoginURL) {
        this.defaultSuccessLoginURL = defaultSuccessLoginURL;
    }

    @Override
    public void setDefaultFailureLoginURL(final String defaultFailureLoginURL) {
        this.defaultFailureLoginURL = defaultFailureLoginURL;
    }

    @Override
    public boolean addAuthPostProcessing(final Implementation authPreProcessing) {
        checkType(authPreProcessing, JPAImplementation.class);
        checkImplementationType(authPreProcessing, AMImplementationType.AUTH_POST_PROCESSING);
        return authenticationPostProcessing.contains((JPAImplementation) authPreProcessing)
                || authenticationPostProcessing.add((JPAImplementation) authPreProcessing);
    }
}
