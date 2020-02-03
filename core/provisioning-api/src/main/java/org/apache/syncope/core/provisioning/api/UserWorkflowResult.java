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
package org.apache.syncope.core.provisioning.api;

import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;

public class UserWorkflowResult<T> extends WorkflowResult<T> {

    private final PropagationByResource<Pair<String, String>> propByLinkedAccount;

    public UserWorkflowResult(
            final T result,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final String performedTask) {

        super(result, propByRes, performedTask);
        this.propByLinkedAccount = propByLinkedAccount;
    }

    public UserWorkflowResult(
            final T result,
            final PropagationByResource<String> propByRes,
            final PropagationByResource<Pair<String, String>> propByLinkedAccount,
            final Set<String> performedTasks) {

        super(result, propByRes, performedTasks);
        this.propByLinkedAccount = propByLinkedAccount;
    }

    public PropagationByResource<Pair<String, String>> getPropByLinkedAccount() {
        return propByLinkedAccount;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(propByLinkedAccount).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        final UserWorkflowResult<T> other = (UserWorkflowResult<T>) obj;
        return new EqualsBuilder().
                appendSuper(true).
                append(propByLinkedAccount, other.propByLinkedAccount).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                appendSuper(super.toString()).
                append(propByLinkedAccount).
                build();
    }
}
