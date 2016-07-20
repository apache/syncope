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
package org.apache.syncope.ide.eclipse.plugin.editors;

import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.HTMLAutoEditStrategy;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.HTMLCompletionProcessor;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.HTMLPartitionScanner;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.HTMLScanner;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.HTMLTagDamagerRepairer;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.HTMLTagScanner;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.IHTMLColorConstants;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.InnerCSSScanner;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.InnerJavaScriptScanner;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.JavaScriptDamagerRepairer;
import org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers.SyncopeTagScanner;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class HTMLSourceConfiguration extends TextSourceViewerConfiguration {

    private HTMLTagScanner tagScanner;
    private SyncopeTagScanner syncopeTagScanner;
    private HTMLScanner scanner;
    private RuleBasedScanner commentScanner;
    private RuleBasedScanner scriptScanner;
    private RuleBasedScanner doctypeScanner;
    private RuleBasedScanner directiveScanner;
    private RuleBasedScanner javaScriptScanner;
    private RuleBasedScanner cssScanner;
    private ContentAssistant contentAssistant;
    private HTMLAutoEditStrategy autoEditStrategy;

    public HTMLSourceConfiguration() {
        contentAssistant = new ContentAssistant();
        HTMLCompletionProcessor proc = new HTMLCompletionProcessor();
        proc.setAutoAssistChars("</\"".toCharArray());
        proc.setAssistCloseTag(true);

        contentAssistant.setContentAssistProcessor(proc, IDocument.DEFAULT_CONTENT_TYPE);
        contentAssistant.setContentAssistProcessor(proc, IDocument.DEFAULT_CONTENT_TYPE);
        contentAssistant.setContentAssistProcessor(proc, HTMLPartitionScanner.HTML_TAG);
        contentAssistant.setContentAssistProcessor(proc, HTMLPartitionScanner.PREFIX_TAG);

        contentAssistant.enableAutoActivation(true);
        contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
    }

    protected HTMLAutoEditStrategy createAutoEditStrategy() {
        return new HTMLAutoEditStrategy();
    }

    public final HTMLAutoEditStrategy getAutoEditStrategy() {
        if (this.autoEditStrategy == null) {
            this.autoEditStrategy = createAutoEditStrategy();
        }
        return this.autoEditStrategy;
    }

    public final IAutoEditStrategy[] getAutoEditStrategies(final ISourceViewer sourceViewer,
            final String contentType) {
        return new IAutoEditStrategy[]{ getAutoEditStrategy() };
    }

    @Override
    public IContentAssistant getContentAssistant(final ISourceViewer sourceViewer) {

            contentAssistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
            return contentAssistant;
    }

    protected HTMLTagScanner getTagScanner() {
        if (tagScanner == null) {
            tagScanner = new HTMLTagScanner(false);
            RGB rgb = IHTMLColorConstants.TAG;
            Color color = new Color(Display.getCurrent(), rgb);
            tagScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return tagScanner;
    }

    protected SyncopeTagScanner getSyncopeTagScanner() {
        if (syncopeTagScanner == null) {
            syncopeTagScanner = new SyncopeTagScanner(false);
            RGB rgb = IHTMLColorConstants.HTML_COMMENT;
            Color color = new Color(Display.getCurrent(), rgb);
            syncopeTagScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return syncopeTagScanner;
    }

    protected HTMLScanner getHTMLScanner() {
        if (scanner == null) {
            scanner = new HTMLScanner();
            RGB rgb = IHTMLColorConstants.FOREGROUND;
            Color color = new Color(Display.getCurrent(), rgb);
            scanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return scanner;
    }

    protected RuleBasedScanner getCommentScanner() {
        if (commentScanner == null) {
            commentScanner = new RuleBasedScanner();
            RGB rgb = IHTMLColorConstants.HTML_COMMENT;
            Color color = new Color(Display.getCurrent(), rgb);
            commentScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return commentScanner;
    }

    protected RuleBasedScanner getScriptScanner() {
        if (scriptScanner == null) {
            scriptScanner = new RuleBasedScanner();
            RGB rgb = IHTMLColorConstants.SCRIPT;
            Color color = new Color(Display.getCurrent(), rgb);
            scriptScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return scriptScanner;
    }

    protected RuleBasedScanner getDoctypeScanner() {
        if (doctypeScanner == null) {
            doctypeScanner = new RuleBasedScanner();
            RGB rgb = IHTMLColorConstants.PROC_INSTR;
            Color color = new Color(Display.getCurrent(), rgb);
            doctypeScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return doctypeScanner;
    }

    protected RuleBasedScanner getDirectiveScanner() {
        if (directiveScanner == null) {
            directiveScanner = new RuleBasedScanner();
            RGB rgb = IHTMLColorConstants.SCRIPT;
            Color color = new Color(Display.getCurrent(), rgb);
            directiveScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return directiveScanner;
    }

    protected RuleBasedScanner getJavaScriptScanner() {
        if (javaScriptScanner == null) {
            javaScriptScanner = new InnerJavaScriptScanner();
            RGB rgb = IHTMLColorConstants.FOREGROUND;
            Color color = new Color(Display.getCurrent(), rgb);
            javaScriptScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return javaScriptScanner;
    }

    protected RuleBasedScanner getCSSScanner() {
        if (cssScanner == null) {
            cssScanner = new InnerCSSScanner();
            RGB rgb = IHTMLColorConstants.FOREGROUND;
            Color color = new Color(Display.getCurrent(), rgb);
            cssScanner.setDefaultReturnToken(new Token(new TextAttribute(color)));
        }
        return cssScanner;
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(final ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = new PresentationReconciler();
        DefaultDamagerRepairer dr = null;

        dr = new HTMLTagDamagerRepairer(getTagScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.HTML_TAG);
        reconciler.setRepairer(dr, HTMLPartitionScanner.HTML_TAG);

        dr = new HTMLTagDamagerRepairer(getTagScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.PREFIX_TAG);
        reconciler.setRepairer(dr, HTMLPartitionScanner.PREFIX_TAG);

        dr = new HTMLTagDamagerRepairer(getSyncopeTagScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.SYNCOPE_TAG);
        reconciler.setRepairer(dr, HTMLPartitionScanner.SYNCOPE_TAG);

        dr = new HTMLTagDamagerRepairer(getHTMLScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        dr = new HTMLTagDamagerRepairer(getCommentScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.HTML_COMMENT);
        reconciler.setRepairer(dr, HTMLPartitionScanner.HTML_COMMENT);

        dr = new DefaultDamagerRepairer(getScriptScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.HTML_SCRIPT);
        reconciler.setRepairer(dr, HTMLPartitionScanner.HTML_SCRIPT);

        dr = new DefaultDamagerRepairer(getDoctypeScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.HTML_DOCTYPE);
        reconciler.setRepairer(dr, HTMLPartitionScanner.HTML_DOCTYPE);

        dr = new DefaultDamagerRepairer(getDirectiveScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.HTML_DIRECTIVE);
        reconciler.setRepairer(dr, HTMLPartitionScanner.HTML_DIRECTIVE);

        dr = new JavaScriptDamagerRepairer(getJavaScriptScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.JAVASCRIPT);
        reconciler.setRepairer(dr, HTMLPartitionScanner.JAVASCRIPT);

        dr = new JavaScriptDamagerRepairer(getCSSScanner());
        reconciler.setDamager(dr, HTMLPartitionScanner.HTML_CSS);
        reconciler.setRepairer(dr, HTMLPartitionScanner.HTML_CSS);

        return reconciler;
    }

    public String[] getConfiguredContentTypes(final ISourceViewer sourceViewer) {
        return new String[] {
            IDocument.DEFAULT_CONTENT_TYPE,
            HTMLPartitionScanner.HTML_COMMENT,
            HTMLPartitionScanner.HTML_TAG,
            HTMLPartitionScanner.PREFIX_TAG,
            HTMLPartitionScanner.HTML_SCRIPT,
            HTMLPartitionScanner.HTML_DOCTYPE,
            HTMLPartitionScanner.HTML_DIRECTIVE,
            HTMLPartitionScanner.JAVASCRIPT,
            HTMLPartitionScanner.HTML_CSS,
            HTMLPartitionScanner.SYNCOPE_TAG};
    }
}
