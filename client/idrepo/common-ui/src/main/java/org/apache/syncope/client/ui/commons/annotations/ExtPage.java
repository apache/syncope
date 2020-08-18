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
package org.apache.syncope.client.ui.commons.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtPage {

    /**
     * @return the i18n key for the label shown under the "Extensions" menu item, on the left pane
     */
    String label();

    /**
     * @return the icon shown next to the label, under the "Extensions" menu item, on the left pane;
     * check https://fortawesome.github.io/Font-Awesome/icons/ for more
     */
    String icon() default "fa-circle-o";

    /**
     * @return the entitlement required to access this extension page
     */
    String listEntitlement();

    /**
     * @return the priority used to determine the display order under the "Extensions" menu item, on the left pane; the
     * higher value, the higher rank
     */
    int priority() default 0;
}
