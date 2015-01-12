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
package org.apache.syncope.server.persistence.jpa.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.apache.syncope.server.persistence.api.entity.VirAttr;

@MappedSuperclass
public abstract class AbstractVirAttr extends AbstractEntity<Long> implements VirAttr {

    private static final long serialVersionUID = 5023204776925954907L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Transient
    protected List<String> values = new ArrayList<>();

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public List<String> getValues() {
        return values;
    }

    @Override
    public boolean addValue(final String value) {
        return !values.contains(value) && values.add(value);
    }

    @Override
    public boolean removeValue(final String value) {
        return values.remove(value);
    }
}
