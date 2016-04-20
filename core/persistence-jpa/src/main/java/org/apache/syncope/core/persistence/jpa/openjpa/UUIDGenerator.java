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
package org.apache.syncope.core.persistence.jpa.openjpa;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Fast UUID generator for OpenJPA entities.
 */
public class UUIDGenerator implements Seq {

    private static final RandomBasedGenerator GENERATOR = Generators.randomBasedGenerator();

    private String last;

    @Override
    public void setType(final int i) {
    }

    @Override
    public Object next(final StoreContext sc, final ClassMetaData cmd) {
        last = GENERATOR.generate().toString();
        return last;
    }

    @Override
    public Object current(final StoreContext sc, final ClassMetaData cmd) {
        return last;
    }

    @Override
    public void allocate(final int i, final StoreContext sc, final ClassMetaData cmd) {
    }

    @Override
    public void close() {
    }
}
