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

import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class InnerJavaScriptScanner extends JavaScriptScanner {

    public InnerJavaScriptScanner() {
        super();
    }

    @Override protected List<IRule> createRules() {
        IToken tag = new Token(new TextAttribute(new Color(Display.getCurrent(),
                IHTMLColorConstants.TAG)));
        IToken comment = new Token(new TextAttribute(new Color(Display.getCurrent(), 
                IHTMLColorConstants.JAVA_COMMENT)));
        IToken jsdoc = new Token(new TextAttribute(new Color(Display.getCurrent(),
                IHTMLColorConstants.JSDOC)));
        List<IRule> rules = super.createRules();
        rules.add(new SingleLineRule(" <script", ">", tag));
        rules.add(new SingleLineRule(" </script", ">", tag));
        rules.add(new MultiLineRule("/**", "*/", jsdoc));
        rules.add(new MultiLineRule("/*", "*/", comment));
        return rules;
    }


}
