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
package org.apache.syncope.common.wrap;

import org.apache.syncope.common.AbstractBaseBean;

public abstract class AbstractWrappable<E> extends AbstractBaseBean {

    private static final long serialVersionUID = 1712808704911635170L;

    private E element;

    public static <E, T extends AbstractWrappable<E>> T getInstance(final Class<T> reference, final E element) {
        try {
            T instance = reference.newInstance();
            instance.setElement(element);
            return instance;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not instantiate " + reference.getName(), e);
        }
    }

    public E getElement() {
        return element;
    }

    public void setElement(final E element) {
        this.element = element;
    }
}
