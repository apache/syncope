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
package org.apache.syncope.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.types.SyncopeClientExceptionType;

public class SyncopeClientException extends Exception {

    private static final long serialVersionUID = 3380920886511913475L;

    private SyncopeClientExceptionType type;

    private Set<String> elements;

    public SyncopeClientException() {
        super();
        elements = new HashSet<String>();
    }

    public SyncopeClientException(SyncopeClientExceptionType type) {
        this();
        setType(type);
    }

    public SyncopeClientExceptionType getType() {
        return type;
    }

    public final void setType(SyncopeClientExceptionType type) {
        this.type = type;
    }

    public boolean addElement(String element) {
        return elements.add(element);
    }

    public boolean removeElement(String element) {
        return elements.remove(element);
    }

    public Set<String> getElements() {
        return elements;
    }

    public void setElements(Set<String> elements) {
        this.elements = elements;
    }

    public void setElements(List<String> elements) {
        this.elements.addAll(elements);
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
