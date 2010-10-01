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
package org.syncope.core.workflow;

/**
 * Commonly used workflow constants.
 */
public final class Constants {

    public static final String ACTION_ACTIVATE = "activate";
    public static final String ACTION_GENERATE_TOKEN = "generateToken";
    public static final String ACTION_RESET = "reset";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_VERIFY_TOKEN = "verifyToken";
    public static final String ENTRY = "entry";
    public static final String SYNCOPE_USER = "syncopeUser";
    public static final String SYNCOPE_ROLE = "syncopeRole";
    public static final String MEMBERSHIP = "membership";
    public static final String TOKEN = "token";
    public static final String USER_TO = "userTO";
    public static final String USER_MOD = "userMod";
    public static final String USER_WORKFLOW = "userWorkflow";

    private Constants() {
    }
}
