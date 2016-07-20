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

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class HTMLTagScanner extends RuleBasedScanner {

    public HTMLTagScanner(final boolean bold) {
        IToken string = null;
        if (bold) {
            RGB rgb = IHTMLColorConstants.TAGLIB_ATTR;
            Color color = new Color(Display.getCurrent(), rgb);
            string = new Token(new TextAttribute(color));
        } else {
            RGB rgb = IHTMLColorConstants.STRING;
            Color color = new Color(Display.getCurrent(), rgb);
            string = new Token(new TextAttribute(color));
        }
        IRule[] rules = new IRule[3];
        rules[0] = new MultiLineRule("\"" , "\"" , string, '\\');
        rules[1] = new MultiLineRule("'"  , "'"  , string, '\\');
        rules[2] = new WhitespaceRule(new HTMLWhitespaceDetector());
        setRules(rules);
    }
}
