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
package org.apache.syncope.client.console.wizards.any;

import java.io.Serializable;
import org.apache.syncope.common.lib.to.AnyTO;

public class AnyWrapper<T extends AnyTO> implements Serializable {

    private static final long serialVersionUID = 8058288034211558375L;

    protected final T anyTO;

    public AnyWrapper(final T anyTO) {
        this.anyTO = anyTO;
    }

    public T getInnerObject() {
        return anyTO;
    }
}
