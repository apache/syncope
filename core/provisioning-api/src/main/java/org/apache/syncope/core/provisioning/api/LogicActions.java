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
package org.apache.syncope.core.provisioning.api;

import java.util.List;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationStatus;

/**
 * Interface for actions to be performed during business logic execution.
 */
public interface LogicActions {

    default <C extends AnyCR> C beforeCreate(C input) {
        return input;
    }

    default <A extends AnyTO> A afterCreate(A input, List<PropagationStatus> statuses) {
        return input;
    }

    default <U extends AnyUR> U beforeUpdate(U input) {
        return input;
    }

    default <A extends AnyTO> A afterUpdate(A input, List<PropagationStatus> statuses) {
        return input;
    }

    default <A extends AnyTO> A beforeDelete(A input) {
        return input;
    }

    default <A extends AnyTO> A afterDelete(A input, List<PropagationStatus> statuses) {
        return input;
    }
}
