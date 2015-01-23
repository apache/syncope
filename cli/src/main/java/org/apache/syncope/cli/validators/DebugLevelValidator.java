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
package org.apache.syncope.cli.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class DebugLevelValidator implements IParameterValidator {

    @Override
    public void validate(final String name, final String value) throws ParameterException {
        if (!Levels.contains(value)) {
            final StringBuilder exceptionMessage = new StringBuilder();
            exceptionMessage.append("Parameter ")
                    .append(name)
                    .append(" should be :\n");
            for (final Levels l : Levels.values()) {
                exceptionMessage.append(l).append("\n");
            }
            System.out.println(">>>> " + exceptionMessage.toString());
        }
    }

    private enum Levels {

        OFF,
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
        ALL;

        public static boolean contains(final String name) {
            for (final Levels c : Levels.values()) {
                if (c.name().equals(name)) {
                    return true;
                }
            }
            return false;
        }
    }

}
