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
package org.syncope.client.validation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.syncope.types.SyncopeClientExceptionType;

public class SyncopeClientCompositeErrorException
        extends HttpClientErrorException {

    private Set<SyncopeClientException> exceptions;

    public SyncopeClientCompositeErrorException(HttpStatus statusCode) {
        super(statusCode);
        exceptions = new HashSet<SyncopeClientException>();
    }

    public boolean hasExceptions() {
        return !exceptions.isEmpty();
    }

    public boolean hasException(SyncopeClientExceptionType exceptionType) {
        return getException(exceptionType) != null;
    }

    public SyncopeClientException getException(
            SyncopeClientExceptionType exceptionType) {

        boolean found = false;
        SyncopeClientException syncopeClientException = null;
        for (Iterator<SyncopeClientException> itor = exceptions.iterator();
                itor.hasNext() && !found;) {

            syncopeClientException = itor.next();
            if (syncopeClientException.getType().equals(exceptionType)) {
                found = true;
            }
        }

        return found ? syncopeClientException : null;
    }

    public Set<SyncopeClientException> getExceptions() {
        return exceptions;
    }

    public boolean addException(SyncopeClientException exception) {
        if (exception.getType() == null) {
            throw new IllegalArgumentException(exception
                    + " does not have the right "
                    + SyncopeClientExceptionType.class.getName() + " set");
        }

        return exceptions.add(exception);
    }

    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder();

        message.append("{");
        for (SyncopeClientException e : getExceptions()) {
            message.append("[");
            message.append(e.getType());
            message.append(" ");
            message.append(e.getElements());
            message.append("], ");
        }
        message.append("}");

        return message.toString();
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}
