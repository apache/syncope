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
package org.apache.syncope.common.lib.clientapps;

import java.util.Objects;
import org.apache.syncope.common.lib.types.PersistentIdGenerator;

public class PairwiseOidcUsernameAttributeProviderConf implements UsernameAttributeProviderConf {

    private static final long serialVersionUID = -4762223354637243358L;

    private PersistentIdGenerator persistentIdGenerator = PersistentIdGenerator.OIDC;

    public PersistentIdGenerator getPersistentIdGenerator() {
        return persistentIdGenerator;
    }

    public void setPersistentIdGenerator(final PersistentIdGenerator persistentIdGenerator) {
        this.persistentIdGenerator = persistentIdGenerator;
    }

    @Override
    public void map(final Mapper mapper) {
        mapper.map(this);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.persistentIdGenerator);
        return hash;
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
        final PairwiseOidcUsernameAttributeProviderConf other =
                (PairwiseOidcUsernameAttributeProviderConf) obj;
        return Objects.equals(this.persistentIdGenerator, other.persistentIdGenerator);
    }
}
