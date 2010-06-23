/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.syncope.core.persistence.Attributable;

public abstract class AbstractController {

    protected static final Logger log = LoggerFactory.getLogger(
            AbstractController.class);

    protected Attributable getAttributable(String kind) {
        Attributable result = null;

        try {
            result = Attributable.valueOf(kind.toUpperCase());
        } catch (Exception e) {
            log.error("Attributable not supported: " + kind);
            throw new TypeMismatchException(kind, Attributable.class, e);
        }

        return result;
    }
}
