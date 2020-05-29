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
package org.apache.syncope.common.lib.request;

import org.apache.syncope.common.lib.types.PatchOperation;

/**
 * When a field of this type from {@link AnyUR}, {@link AnyObjectUR}, {@link GroupUR} or {@link UserUR} is
 * {@code null}, no change will be performed; otherwise the result from {@link #getValue()} will replace the current
 * value of the related field.
 *
 * @param <T> {@code String}, {@code Boolean}
 */
public abstract class AbstractReplacePatchItem<T> extends AbstractPatchItem<T> {

    private static final long serialVersionUID = 2027599764019829563L;

    public AbstractReplacePatchItem() {
        super();
        super.setOperation(PatchOperation.ADD_REPLACE);
    }

    @Override
    public void setOperation(final PatchOperation operation) {
        // fixed
    }
}
