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
package org.apache.syncope.common;

public abstract class AbstractWrappable extends AbstractBaseBean {

    private static final long serialVersionUID = 1712808704911635170L;

    private String name;

    public static <T extends AbstractWrappable> T getInstance(final Class<T> reference, final String name) {
        try {
            T instance = reference.newInstance();
            instance.setName(name);
            return instance;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not instantiate " + reference.getName(), e);
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }
}
