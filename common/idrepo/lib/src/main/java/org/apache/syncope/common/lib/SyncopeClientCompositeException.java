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
package org.apache.syncope.common.lib;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.ClientExceptionType;

public class SyncopeClientCompositeException extends SyncopeClientException {

    private static final long serialVersionUID = 7882118041134372129L;

    private final Set<SyncopeClientException> exceptions = new HashSet<>();

    protected SyncopeClientCompositeException() {
        super(ClientExceptionType.Composite);
    }

    public boolean hasExceptions() {
        return !exceptions.isEmpty();
    }

    public Optional<SyncopeClientException> getException(final ClientExceptionType exceptionType) {
        return exceptions.stream().filter(e -> e.getType() == exceptionType).findFirst();
    }

    public Set<SyncopeClientException> getExceptions() {
        return exceptions;
    }

    public boolean addException(final SyncopeClientException exception) {
        if (exception.getType() == null) {
            exception.setType(ClientExceptionType.Unknown);
        }

        return exceptions.stream().
                filter(e -> e.getType() == exception.getType()).findFirst().
                map(e -> e.getElements().addAll(exception.getElements())).
                orElseGet(() -> exceptions.add(exception));
    }

    @Override
    public String getMessage() {
        return new StringBuilder().
                append('{').
                append(getExceptions().stream().map(e -> '[' + e.getMessage() + ']').collect(Collectors.joining(", "))).
                append('}').
                toString();
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}
