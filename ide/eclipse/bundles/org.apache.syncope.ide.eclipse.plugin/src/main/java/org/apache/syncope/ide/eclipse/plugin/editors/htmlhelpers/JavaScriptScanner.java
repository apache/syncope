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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class JavaScriptScanner extends RuleBasedScanner {

    public static final String[] KEYWORDS = {
            "abstract",
            "boolean", "break", "byte",
            "case", "catch", "char", "class", "const", "continue",
            "debugger", "default", "delete", "do", "double",
            "else", "enum", "export", "extends",
            "false", "final", "finally", "float", "for", "function",
            "goto", "if", "implements", "import", "in", "instanceof", "int", "interface",
            "let", "long",
            "native", "new", "null",
            "package", "private", "protected", "prototype", "public",
            "return", "short", "static", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "true", "try", "typeof",
            "var", "void", "while", "with",
            "typeof", "yield", "undefined", "Infinity", "NaN"
    };

    public JavaScriptScanner() {
        List<IRule> rules = createRules();
        setRules(rules.toArray(new IRule[rules.size()]));
    }

    /**
     * Creates the list of <code>IRule</code>.
     * If you have to customize rules, override this method.
     *
     * @return the list of <code>IRule</code>
     */
    protected List<IRule> createRules() {
        IToken normal  = new Token(new TextAttribute(new Color(Display.getCurrent(),
                IHTMLColorConstants.FOREGROUND)));
        IToken string  = new Token(new TextAttribute(new Color(Display.getCurrent(),
                IHTMLColorConstants.JAVA_STRING)));
        IToken comment = new Token(new TextAttribute(new Color(Display.getCurrent(),
                IHTMLColorConstants.JAVA_COMMENT)));
        IToken keyword = new Token(new TextAttribute(new Color(Display.getCurrent(),
                IHTMLColorConstants.JAVA_KEYWORD)));
        List<IRule> rules = new ArrayList<IRule>();
        rules.add(new SingleLineRule("\"", "\"", string, '\\'));
        rules.add(new SingleLineRule("'", "'", string, '\\'));
        rules.add(new SingleLineRule("\\//", null, normal));
        rules.add(new EndOfLineRule("//", comment));
        WordRule wordRule = new WordRule(new JavaWordDetector(), normal);
        for (int i = 0; i < KEYWORDS.length; i++) {
            wordRule.addWord(KEYWORDS[i], keyword);
        }
        rules.add(wordRule);
        return rules;
    }

}
