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

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class WorkflowResult<T> {

    private T result;

    private final PropagationByResource<String> propByRes;

    private final Set<String> performedTasks = new HashSet<>();

    public WorkflowResult(
            final T result,
            final PropagationByResource<String> propByRes,
            final String performedTask) {

        this.result = result;
        this.propByRes = propByRes;
        this.performedTasks.add(performedTask);
    }

    public WorkflowResult(
            final T result,
            final PropagationByResource<String> propByRes,
            final Set<String> performedTasks) {

        this.result = result;
        this.propByRes = propByRes;
        this.performedTasks.addAll(performedTasks);
    }

    public T getResult() {
        return result;
    }

    public void setResult(final T result) {
        this.result = result;
    }

    public Set<String> getPerformedTasks() {
        return performedTasks;
    }

    public PropagationByResource<String> getPropByRes() {
        return propByRes;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(result).
                append(propByRes).
                append(performedTasks).
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
        final WorkflowResult<T> other = (WorkflowResult<T>) obj;
        return new EqualsBuilder().
                append(result, other.result).
                append(propByRes, other.propByRes).
                append(performedTasks, other.performedTasks).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(result).
                append(propByRes).
                append(performedTasks).
                build();
    }
}
