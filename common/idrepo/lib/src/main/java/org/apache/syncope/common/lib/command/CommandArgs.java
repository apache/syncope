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
package org.apache.syncope.common.lib.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.syncope.common.lib.BaseBean;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
public class CommandArgs implements BaseBean {

    private static final long serialVersionUID = -85050010490462751L;

    public record Result(String message, Map<String, Serializable> values) implements Serializable {

        public static final Result EMPTY = new Result(StringUtils.EMPTY, Map.of());

        public Result(final String message) {
            this(message, Map.of());
        }

        public boolean isEmpty() {
            return StringUtils.isBlank(message) && CollectionUtils.isEmpty(values);
        }
    }

    @JsonIgnore
    private Result pipe;

    public final Optional<Result> getPipe() {
        return Optional.ofNullable(pipe);
    }

    public final void setPipe(final Result pipe) {
        this.pipe = pipe;
    }
}
