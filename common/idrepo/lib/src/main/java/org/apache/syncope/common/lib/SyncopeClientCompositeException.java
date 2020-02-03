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
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
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

    public boolean hasException(final ClientExceptionType exceptionType) {
        return getException(exceptionType) != null;
    }

    public SyncopeClientException getException(final ClientExceptionType exceptionType) {
        boolean found = false;
        SyncopeClientException syncopeClientException = null;
        for (Iterator<SyncopeClientException> itor = exceptions.iterator(); itor.hasNext() && !found;) {
            syncopeClientException = itor.next();
            if (syncopeClientException.getType().equals(exceptionType)) {
                found = true;
            }
        }

        return found
                ? syncopeClientException
                : null;
    }

    public Set<SyncopeClientException> getExceptions() {
        return exceptions;
    }

    public boolean addException(final SyncopeClientException exception) {
        if (exception.getType() == null) {
            throw new IllegalArgumentException(exception + " does not have the right "
                    + ClientExceptionType.class.getName() + " set");
        }

        Optional<SyncopeClientException> alreadyAdded =
                exceptions.stream().filter(ex -> ex.getType() == exception.getType()).findFirst();

        return alreadyAdded.map(e -> e.getElements()
            .addAll(exception.getElements())).orElseGet(() -> exceptions.add(exception));
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder();

        message.append('{');
        Iterator<SyncopeClientException> iter = getExceptions().iterator();
        while (iter.hasNext()) {
            SyncopeClientException e = iter.next();
            message.append('[').
                    append(e.getMessage()).
                    append(']');
            if (iter.hasNext()) {
                message.append(", ");
            }
        }
        message.append('}');

        return message.toString();
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}
