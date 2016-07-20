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
package org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers;

import org.eclipse.swt.graphics.RGB;

/** Defines initial colors used in editors. */
public interface IHTMLColorConstants {
    RGB HTML_COMMENT = new RGB(128, 0, 0);
    RGB PROC_INSTR   = new RGB(128, 128, 128);
    RGB STRING       = new RGB(0, 128, 0);
    RGB DEFAULT      = new RGB(0, 0, 0);
    RGB TAG          = new RGB(0, 0, 128);
    RGB SCRIPT       = new RGB(255, 0, 0);
    RGB CSS_PROP     = new RGB(0, 0, 255);
    RGB CSS_COMMENT  = new RGB(128, 0, 0);
    RGB CSS_VALUE    = new RGB(0, 128, 0);
    RGB FOREGROUND   = new RGB(0, 0, 0);
    RGB BACKGROUND   = new RGB(255, 255, 255);
    RGB JAVA_COMMENT = new RGB(0, 128, 0);
    RGB JAVA_STRING  = new RGB(0, 0, 255);
    RGB JAVA_KEYWORD = new RGB(128, 0, 128);
    RGB TAGLIB       = new RGB(255, 0, 0);
    RGB TAGLIB_ATTR  = new RGB(128, 128, 0);
    RGB JSDOC        = new RGB(128, 128, 255);
}
