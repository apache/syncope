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

public final class TagDefinition {

    private static List<TagInfo> TAG_LIST = new ArrayList<TagInfo>();

    private TagDefinition() {
    }

    public static TagInfo[] getTagInfoAsArray() {
        return TAG_LIST.toArray(new TagInfo[TAG_LIST.size()]);
    }

    public static List<TagInfo> getTagInfoAsList() {
        return TAG_LIST;
    }

    private static void addTagInfo(final TagInfo tagInfo) {
        TAG_LIST.add(tagInfo);
    }

    private static void addEventAttr(final TagInfo tagInfo) {
        tagInfo.addAttributeInfo(new AttributeInfo("onclick", true));
        tagInfo.addAttributeInfo(new AttributeInfo("ondblclick", true));
        tagInfo.addAttributeInfo(new AttributeInfo("onmousedown", true));
        tagInfo.addAttributeInfo(new AttributeInfo("onmouseup", true));
        tagInfo.addAttributeInfo(new AttributeInfo("onmouseover", true));
        tagInfo.addAttributeInfo(new AttributeInfo("onmousemove", true));
        tagInfo.addAttributeInfo(new AttributeInfo("onmouseout", true));
        tagInfo.addAttributeInfo(new AttributeInfo("onkeypress", true));
        tagInfo.addAttributeInfo(new AttributeInfo("onkeydown", true));
        tagInfo.addAttributeInfo(new AttributeInfo("onkeyup", true));
    }

    static {
        TagInfo a = new TagInfo("a", true);
        a.addAttributeInfo(new AttributeInfo("href", true,
            AttributeInfo.AttributeTypeOptions.FILE));
        a.addAttributeInfo(new AttributeInfo("name", true));
        a.addAttributeInfo(new AttributeInfo("target", true,
            AttributeInfo.AttributeTypeOptions.TARGET));
        a.addAttributeInfo(new AttributeInfo("charset", true));
        a.addAttributeInfo(new AttributeInfo("hreflang", true));
        a.addAttributeInfo(new AttributeInfo("methods", true));
        a.addAttributeInfo(new AttributeInfo("rel", true));
        a.addAttributeInfo(new AttributeInfo("rev", true));
        a.addAttributeInfo(new AttributeInfo("type", true));
        a.addAttributeInfo(new AttributeInfo("urn", true));
        a.addAttributeInfo(new AttributeInfo("coords", true));
        a.addAttributeInfo(new AttributeInfo("shape", true));
        a.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        a.addAttributeInfo(new AttributeInfo("dir", true));
        a.addAttributeInfo(new AttributeInfo("id", true));
        a.addAttributeInfo(new AttributeInfo("lang", true));
        a.addAttributeInfo(new AttributeInfo("style", true));
        a.addAttributeInfo(new AttributeInfo("title", true));
        a.addAttributeInfo(new AttributeInfo("onfocus", true));
        a.addAttributeInfo(new AttributeInfo("onblur", true));
        addEventAttr(a);
        addTagInfo(a);

        TagInfo abbr = new TagInfo("abbr", true);
        abbr.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        abbr.addAttributeInfo(new AttributeInfo("dir", true));
        abbr.addAttributeInfo(new AttributeInfo("id", true));
        abbr.addAttributeInfo(new AttributeInfo("lang", true));
        abbr.addAttributeInfo(new AttributeInfo("style", true));
        abbr.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(abbr);
        addTagInfo(abbr);

        TagInfo acronym = new TagInfo("acronym", true);
        acronym.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        acronym.addAttributeInfo(new AttributeInfo("dir", true));
        acronym.addAttributeInfo(new AttributeInfo("id", true));
        acronym.addAttributeInfo(new AttributeInfo("lang", true));
        acronym.addAttributeInfo(new AttributeInfo("style", true));
        acronym.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(acronym);
        addTagInfo(acronym);

        TagInfo address = new TagInfo("address", true);
        address.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        address.addAttributeInfo(new AttributeInfo("dir", true));
        address.addAttributeInfo(new AttributeInfo("id", true));
        address.addAttributeInfo(new AttributeInfo("lang", true));
        address.addAttributeInfo(new AttributeInfo("style", true));
        address.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(address);
        addTagInfo(address);

        TagInfo applet = new TagInfo("applet", true);
        applet.addAttributeInfo(new AttributeInfo("alt", true));
        applet.addAttributeInfo(new AttributeInfo("archive", true));
        applet.addAttributeInfo(new AttributeInfo("code", true));
        applet.addAttributeInfo(new AttributeInfo("codebase", true));
        applet.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        applet.addAttributeInfo(new AttributeInfo("height", true));
        applet.addAttributeInfo(new AttributeInfo("hspace", true));
        applet.addAttributeInfo(new AttributeInfo("vspace", true));
        applet.addAttributeInfo(new AttributeInfo("width", true));
        applet.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        applet.addAttributeInfo(new AttributeInfo("id", true));
        applet.addAttributeInfo(new AttributeInfo("name", true));
        applet.addAttributeInfo(new AttributeInfo("style", true));
        applet.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(applet);

        TagInfo area = new TagInfo("area", false, true);
        area.addAttributeInfo(new AttributeInfo("shape", true));
        area.addAttributeInfo(new AttributeInfo("coords", true));
        area.addAttributeInfo(new AttributeInfo("href", true));
        area.addAttributeInfo(new AttributeInfo("alt", true));
        area.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        area.addAttributeInfo(new AttributeInfo("dir", true));
        area.addAttributeInfo(new AttributeInfo("id", true));
        area.addAttributeInfo(new AttributeInfo("lang", true));
        area.addAttributeInfo(new AttributeInfo("language", true));
        area.addAttributeInfo(new AttributeInfo("name", true));
        area.addAttributeInfo(new AttributeInfo("style", true));
        area.addAttributeInfo(new AttributeInfo("title", true));
        area.addAttributeInfo(new AttributeInfo("onfocus", true));
        area.addAttributeInfo(new AttributeInfo("onblur", true));
        addEventAttr(area);
        addTagInfo(area);

        TagInfo b = new TagInfo("b", true);
        b.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        b.addAttributeInfo(new AttributeInfo("dir", true));
        b.addAttributeInfo(new AttributeInfo("id", true));
        b.addAttributeInfo(new AttributeInfo("lang", true));
        b.addAttributeInfo(new AttributeInfo("style", true));
        b.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(b);
        addTagInfo(b);

        TagInfo base = new TagInfo("base", false, true);
        base.addAttributeInfo(new AttributeInfo("href", true));
        base.addAttributeInfo(new AttributeInfo("target", true));
        addTagInfo(base);

        TagInfo basefont = new TagInfo("basefont", false, true);
        basefont.addAttributeInfo(new AttributeInfo("size", true));
        basefont.addAttributeInfo(new AttributeInfo("color", true));
        basefont.addAttributeInfo(new AttributeInfo("face", true));
        basefont.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        basefont.addAttributeInfo(new AttributeInfo("id", true));
        basefont.addAttributeInfo(new AttributeInfo("lang", true));
        basefont.addAttributeInfo(new AttributeInfo("style", true));
        addTagInfo(basefont);

        TagInfo bdo = new TagInfo("bdo", true);
        bdo.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        bdo.addAttributeInfo(new AttributeInfo("dir", true));
        bdo.addAttributeInfo(new AttributeInfo("id", true));
        bdo.addAttributeInfo(new AttributeInfo("lang", true));
        bdo.addAttributeInfo(new AttributeInfo("style", true));
        bdo.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(bdo);

        TagInfo bgsound = new TagInfo("bgsound", false);
        bgsound.addAttributeInfo(new AttributeInfo("balance", true));
        bgsound.addAttributeInfo(new AttributeInfo("loop", true));
        bgsound.addAttributeInfo(new AttributeInfo("src", true));
        bgsound.addAttributeInfo(new AttributeInfo("volume", true));
        bgsound.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        bgsound.addAttributeInfo(new AttributeInfo("id", true));
        bgsound.addAttributeInfo(new AttributeInfo("lang", true));
        bgsound.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(bgsound);

        TagInfo big = new TagInfo("big", true);
        big.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        big.addAttributeInfo(new AttributeInfo("dir", true));
        big.addAttributeInfo(new AttributeInfo("id", true));
        big.addAttributeInfo(new AttributeInfo("lang", true));
        big.addAttributeInfo(new AttributeInfo("style", true));
        big.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(big);
        addTagInfo(big);

        TagInfo blink = new TagInfo("blink", true);
        blink.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        blink.addAttributeInfo(new AttributeInfo("dir", true));
        blink.addAttributeInfo(new AttributeInfo("id", true));
        blink.addAttributeInfo(new AttributeInfo("lang", true));
        blink.addAttributeInfo(new AttributeInfo("style", true));
        blink.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(blink);
        addTagInfo(blink);

        TagInfo blockquote = new TagInfo("blockquote", true);
        blockquote.addAttributeInfo(new AttributeInfo("cite", true));
        blockquote.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        blockquote.addAttributeInfo(new AttributeInfo("dir", true));
        blockquote.addAttributeInfo(new AttributeInfo("id", true));
        blockquote.addAttributeInfo(new AttributeInfo("lang", true));
        blockquote.addAttributeInfo(new AttributeInfo("style", true));
        blockquote.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(blockquote);
        addTagInfo(blockquote);

        TagInfo body = new TagInfo("body", true);
        body.addAttributeInfo(new AttributeInfo("text", true));
        body.addAttributeInfo(new AttributeInfo("link", true));
        body.addAttributeInfo(new AttributeInfo("vlink", true));
        body.addAttributeInfo(new AttributeInfo("alink", true));
        body.addAttributeInfo(new AttributeInfo("bgcolor", true));
        body.addAttributeInfo(new AttributeInfo("background", true));
        body.addAttributeInfo(new AttributeInfo("marginheight", true));
        body.addAttributeInfo(new AttributeInfo("marginwidth", true));
        body.addAttributeInfo(new AttributeInfo("topmargin", true));
        body.addAttributeInfo(new AttributeInfo("leftmargin", true));
        body.addAttributeInfo(new AttributeInfo("bottommargin", true));
        body.addAttributeInfo(new AttributeInfo("rightmargin", true));
        body.addAttributeInfo(new AttributeInfo("scroll", true));
        body.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        body.addAttributeInfo(new AttributeInfo("dir", true));
        body.addAttributeInfo(new AttributeInfo("id", true));
        body.addAttributeInfo(new AttributeInfo("lang", true));
        body.addAttributeInfo(new AttributeInfo("style", true));
        body.addAttributeInfo(new AttributeInfo("title", true));
        body.addAttributeInfo(new AttributeInfo("onload", true));
        body.addAttributeInfo(new AttributeInfo("onunload", true));
        addTagInfo(body);

        TagInfo br = new TagInfo("br", false, true);
        br.addAttributeInfo(new AttributeInfo("clear", true));
        br.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        br.addAttributeInfo(new AttributeInfo("id", true));
        br.addAttributeInfo(new AttributeInfo("language", true));
        br.addAttributeInfo(new AttributeInfo("style", true));
        br.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(br);

        TagInfo button = new TagInfo("button", true);
        button.addAttributeInfo(new AttributeInfo("disabled", false));
        button.addAttributeInfo(new AttributeInfo("type", true));
        button.addAttributeInfo(new AttributeInfo("value", true));
        button.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        button.addAttributeInfo(new AttributeInfo("dir", true));
        button.addAttributeInfo(new AttributeInfo("id", true));
        button.addAttributeInfo(new AttributeInfo("lang", true));
        button.addAttributeInfo(new AttributeInfo("name", true));
        button.addAttributeInfo(new AttributeInfo("style", true));
        button.addAttributeInfo(new AttributeInfo("title", true));
        button.addAttributeInfo(new AttributeInfo("onfocus", true));
        button.addAttributeInfo(new AttributeInfo("onblur", true));
        addEventAttr(button);
        addTagInfo(button);

        TagInfo caption = new TagInfo("caption", true);
        caption.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        caption.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        caption.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        caption.addAttributeInfo(new AttributeInfo("dir", true));
        caption.addAttributeInfo(new AttributeInfo("id", true));
        caption.addAttributeInfo(new AttributeInfo("lang", true));
        caption.addAttributeInfo(new AttributeInfo("style", true));
        caption.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(caption);
        addTagInfo(caption);

        TagInfo center = new TagInfo("center", true);
        center.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        center.addAttributeInfo(new AttributeInfo("dir", true));
        center.addAttributeInfo(new AttributeInfo("id", true));
        center.addAttributeInfo(new AttributeInfo("lang", true));
        center.addAttributeInfo(new AttributeInfo("style", true));
        center.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(center);
        addTagInfo(center);

        TagInfo cite = new TagInfo("cite", true);
        cite.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        cite.addAttributeInfo(new AttributeInfo("dir", true));
        cite.addAttributeInfo(new AttributeInfo("id", true));
        cite.addAttributeInfo(new AttributeInfo("lang", true));
        cite.addAttributeInfo(new AttributeInfo("style", true));
        cite.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(cite);
        addTagInfo(cite);

        TagInfo code = new TagInfo("code", true);
        code.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        code.addAttributeInfo(new AttributeInfo("dir", true));
        code.addAttributeInfo(new AttributeInfo("id", true));
        code.addAttributeInfo(new AttributeInfo("lang", true));
        code.addAttributeInfo(new AttributeInfo("style", true));
        code.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(code);
        addTagInfo(code);

        TagInfo col = new TagInfo("col", false, true);
        col.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        col.addAttributeInfo(new AttributeInfo("bgcolor", true));
        col.addAttributeInfo(new AttributeInfo("char", true));
        col.addAttributeInfo(new AttributeInfo("charoff", true));
        col.addAttributeInfo(new AttributeInfo("span", true));
        col.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        col.addAttributeInfo(new AttributeInfo("width", true));
        col.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        col.addAttributeInfo(new AttributeInfo("dir", true));
        col.addAttributeInfo(new AttributeInfo("id", true));
        col.addAttributeInfo(new AttributeInfo("lang", true));
        col.addAttributeInfo(new AttributeInfo("style", true));
        col.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(col);
        addTagInfo(col);

        TagInfo colgroup = new TagInfo("colgroup", true);
        colgroup.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        colgroup.addAttributeInfo(new AttributeInfo("bgcolor", true));
        colgroup.addAttributeInfo(new AttributeInfo("char", true));
        colgroup.addAttributeInfo(new AttributeInfo("charoff", true));
        colgroup.addAttributeInfo(new AttributeInfo("span", true));
        colgroup.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        colgroup.addAttributeInfo(new AttributeInfo("width", true));
        colgroup.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        colgroup.addAttributeInfo(new AttributeInfo("dir", true));
        colgroup.addAttributeInfo(new AttributeInfo("id", true));
        colgroup.addAttributeInfo(new AttributeInfo("lang", true));
        colgroup.addAttributeInfo(new AttributeInfo("style", true));
        colgroup.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(colgroup);
        addTagInfo(colgroup);

        TagInfo comment = new TagInfo("comment", true);
        comment.addAttributeInfo(new AttributeInfo("id", true));
        comment.addAttributeInfo(new AttributeInfo("lang", true));
        addTagInfo(comment);

        TagInfo dd = new TagInfo("dd", true);
        dd.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        dd.addAttributeInfo(new AttributeInfo("dir", true));
        dd.addAttributeInfo(new AttributeInfo("id", true));
        dd.addAttributeInfo(new AttributeInfo("lang", true));
        dd.addAttributeInfo(new AttributeInfo("style", true));
        dd.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(dd);
        addTagInfo(dd);

        TagInfo del = new TagInfo("del", true);
        del.addAttributeInfo(new AttributeInfo("cite", true));
        del.addAttributeInfo(new AttributeInfo("datetime", true));
        del.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        del.addAttributeInfo(new AttributeInfo("dir", true));
        del.addAttributeInfo(new AttributeInfo("id", true));
        del.addAttributeInfo(new AttributeInfo("lang", true));
        del.addAttributeInfo(new AttributeInfo("style", true));
        del.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(del);
        addTagInfo(del);

        TagInfo dfn = new TagInfo("dfn", true);
        dfn.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        dfn.addAttributeInfo(new AttributeInfo("dir", true));
        dfn.addAttributeInfo(new AttributeInfo("id", true));
        dfn.addAttributeInfo(new AttributeInfo("lang", true));
        dfn.addAttributeInfo(new AttributeInfo("style", true));
        dfn.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(dfn);
        addTagInfo(dfn);

        TagInfo dir = new TagInfo("dir", true);
        dir.addAttributeInfo(new AttributeInfo("compact", false));
        dir.addAttributeInfo(new AttributeInfo("type", true));
        dir.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        dir.addAttributeInfo(new AttributeInfo("dir", true));
        dir.addAttributeInfo(new AttributeInfo("id", true));
        dir.addAttributeInfo(new AttributeInfo("lang", true));
        dir.addAttributeInfo(new AttributeInfo("style", true));
        dir.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(dir);

        TagInfo div = new TagInfo("div", true);
        div.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        div.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        div.addAttributeInfo(new AttributeInfo("dir", true));
        div.addAttributeInfo(new AttributeInfo("id", true));
        div.addAttributeInfo(new AttributeInfo("lang", true));
        div.addAttributeInfo(new AttributeInfo("style", true));
        div.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(div);
        addTagInfo(div);

        TagInfo dl = new TagInfo("dl", true);
        dl.addAttributeInfo(new AttributeInfo("compact", false));
        dl.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        dl.addAttributeInfo(new AttributeInfo("dir", true));
        dl.addAttributeInfo(new AttributeInfo("id", true));
        dl.addAttributeInfo(new AttributeInfo("lang", true));
        dl.addAttributeInfo(new AttributeInfo("style", true));
        dl.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(dl);
        addTagInfo(dl);

        TagInfo dt = new TagInfo("dt", true);
        dt.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        dt.addAttributeInfo(new AttributeInfo("dir", true));
        dt.addAttributeInfo(new AttributeInfo("id", true));
        dt.addAttributeInfo(new AttributeInfo("lang", true));
        dt.addAttributeInfo(new AttributeInfo("style", true));
        dt.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(dt);
        addTagInfo(dt);

        TagInfo em = new TagInfo("em", true);
        em.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        em.addAttributeInfo(new AttributeInfo("dir", true));
        em.addAttributeInfo(new AttributeInfo("id", true));
        em.addAttributeInfo(new AttributeInfo("lang", true));
        em.addAttributeInfo(new AttributeInfo("style", true));
        em.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(em);
        addTagInfo(em);

        TagInfo embed = new TagInfo("embed", false);
        embed.addAttributeInfo(new AttributeInfo("src", true));
        embed.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        embed.addAttributeInfo(new AttributeInfo("height", true));
        embed.addAttributeInfo(new AttributeInfo("hspace", true));
        embed.addAttributeInfo(new AttributeInfo("vspace", true));
        embed.addAttributeInfo(new AttributeInfo("units", true));
        embed.addAttributeInfo(new AttributeInfo("border", true));
        embed.addAttributeInfo(new AttributeInfo("frameborder", true));
        embed.addAttributeInfo(new AttributeInfo("hidden", true));
        embed.addAttributeInfo(new AttributeInfo("alt", true));
        embed.addAttributeInfo(new AttributeInfo("code", true));
        embed.addAttributeInfo(new AttributeInfo("codebase", true));
        embed.addAttributeInfo(new AttributeInfo("type", true));
        embed.addAttributeInfo(new AttributeInfo("palette", true));
        embed.addAttributeInfo(new AttributeInfo("pluginspace", true));
        embed.addAttributeInfo(new AttributeInfo("pluginurl", true));
        embed.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        embed.addAttributeInfo(new AttributeInfo("id", true));
        embed.addAttributeInfo(new AttributeInfo("name", true));
        embed.addAttributeInfo(new AttributeInfo("style", true));
        embed.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(embed);

        TagInfo fieldset = new TagInfo("fieldset", true);
        fieldset.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        fieldset.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        fieldset.addAttributeInfo(new AttributeInfo("dir", true));
        fieldset.addAttributeInfo(new AttributeInfo("id", true));
        fieldset.addAttributeInfo(new AttributeInfo("lang", true));
        fieldset.addAttributeInfo(new AttributeInfo("style", true));
        fieldset.addAttributeInfo(new AttributeInfo("title", true));
//        fieldset.addAttributeInfo(new AttributeInfo("onfocus", true));
//        fieldset.addAttributeInfo(new AttributeInfo("onblur", true));
        addEventAttr(fieldset);
        addTagInfo(fieldset);

        TagInfo font = new TagInfo("font", true);
        font.addAttributeInfo(new AttributeInfo("size", true));
        font.addAttributeInfo(new AttributeInfo("color", true));
        font.addAttributeInfo(new AttributeInfo("face", true));
        font.addAttributeInfo(new AttributeInfo("font-weight", true));
        font.addAttributeInfo(new AttributeInfo("point-size", true));
        font.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        font.addAttributeInfo(new AttributeInfo("dir", true));
        font.addAttributeInfo(new AttributeInfo("id", true));
        font.addAttributeInfo(new AttributeInfo("lang", true));
        font.addAttributeInfo(new AttributeInfo("style", true));
        font.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(font);
        addTagInfo(font);

        TagInfo form = new TagInfo("form", true);
        form.addAttributeInfo(new AttributeInfo("action", true,
            AttributeInfo.AttributeTypeOptions.FILE));
        form.addAttributeInfo(new AttributeInfo("method", true));
        form.addAttributeInfo(new AttributeInfo("target", true));
        form.addAttributeInfo(new AttributeInfo("accept", true));
        form.addAttributeInfo(new AttributeInfo("accept-charset", true));
        form.addAttributeInfo(new AttributeInfo("autocomplete", true));
        form.addAttributeInfo(new AttributeInfo("enctype", true));
        form.addAttributeInfo(new AttributeInfo("urn", false));
        form.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        form.addAttributeInfo(new AttributeInfo("dir", true));
        form.addAttributeInfo(new AttributeInfo("id", true));
        form.addAttributeInfo(new AttributeInfo("lang", true));
        form.addAttributeInfo(new AttributeInfo("name", true));
        form.addAttributeInfo(new AttributeInfo("style", true));
        form.addAttributeInfo(new AttributeInfo("title", true));
//        form.addAttributeInfo(new AttributeInfo("onfocus", true));
//        form.addAttributeInfo(new AttributeInfo("onblur", true));
        form.addAttributeInfo(new AttributeInfo("onsubmit", true));
        form.addAttributeInfo(new AttributeInfo("onreset", true));
        addEventAttr(form);
        addTagInfo(form);

        TagInfo frame = new TagInfo("frame", false, true);
        frame.addAttributeInfo(new AttributeInfo("src", true,
            AttributeInfo.AttributeTypeOptions.FILE));
        frame.addAttributeInfo(new AttributeInfo("height", true));
        frame.addAttributeInfo(new AttributeInfo("width", true));
        frame.addAttributeInfo(new AttributeInfo("bordercolor", true));
        frame.addAttributeInfo(new AttributeInfo("frameborder", true));
        frame.addAttributeInfo(new AttributeInfo("marginheight", true));
        frame.addAttributeInfo(new AttributeInfo("marginwidth", true));
        frame.addAttributeInfo(new AttributeInfo("noresize", false));
        frame.addAttributeInfo(new AttributeInfo("scrolling", true));
        frame.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        frame.addAttributeInfo(new AttributeInfo("id", true));
        frame.addAttributeInfo(new AttributeInfo("lang", true));
        frame.addAttributeInfo(new AttributeInfo("language", true));
        frame.addAttributeInfo(new AttributeInfo("name", true));
        frame.addAttributeInfo(new AttributeInfo("style", true));
        frame.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(frame);

        TagInfo frameset = new TagInfo("frameset", true);
        frameset.addAttributeInfo(new AttributeInfo("border", true));
        frameset.addAttributeInfo(new AttributeInfo("bordercolor", true));
        frameset.addAttributeInfo(new AttributeInfo("cols", true));
        frameset.addAttributeInfo(new AttributeInfo("frameborder", true));
        frameset.addAttributeInfo(new AttributeInfo("framespacing", true));
        frameset.addAttributeInfo(new AttributeInfo("rows", true));
        frameset.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        frameset.addAttributeInfo(new AttributeInfo("id", true));
        frameset.addAttributeInfo(new AttributeInfo("lang", true));
        frameset.addAttributeInfo(new AttributeInfo("language", true));
        frameset.addAttributeInfo(new AttributeInfo("name", true));
        frameset.addAttributeInfo(new AttributeInfo("style", true));
        frameset.addAttributeInfo(new AttributeInfo("title", true));
        frameset.addAttributeInfo(new AttributeInfo("onload", true));
        frameset.addAttributeInfo(new AttributeInfo("onunload", true));
        addTagInfo(frameset);

        TagInfo h1 = new TagInfo("h1", true);
        h1.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        h1.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        h1.addAttributeInfo(new AttributeInfo("dir", true));
        h1.addAttributeInfo(new AttributeInfo("id", true));
        h1.addAttributeInfo(new AttributeInfo("lang", true));
        h1.addAttributeInfo(new AttributeInfo("style", true));
        h1.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(h1);
        addTagInfo(h1);

        TagInfo h2 = new TagInfo("h2", true);
        h2.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        h2.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        h2.addAttributeInfo(new AttributeInfo("dir", true));
        h2.addAttributeInfo(new AttributeInfo("id", true));
        h2.addAttributeInfo(new AttributeInfo("lang", true));
        h2.addAttributeInfo(new AttributeInfo("style", true));
        h2.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(h2);
        addTagInfo(h2);

        TagInfo h3 = new TagInfo("h3", true);
        h3.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        h3.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        h3.addAttributeInfo(new AttributeInfo("dir", true));
        h3.addAttributeInfo(new AttributeInfo("id", true));
        h3.addAttributeInfo(new AttributeInfo("lang", true));
        h3.addAttributeInfo(new AttributeInfo("style", true));
        h3.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(h3);
        addTagInfo(h3);

        TagInfo h4 = new TagInfo("h4", true);
        h4.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        h4.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        h4.addAttributeInfo(new AttributeInfo("dir", true));
        h4.addAttributeInfo(new AttributeInfo("id", true));
        h4.addAttributeInfo(new AttributeInfo("lang", true));
        h4.addAttributeInfo(new AttributeInfo("style", true));
        h4.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(h4);
        addTagInfo(h4);

        TagInfo h5 = new TagInfo("h5", true);
        h5.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        h5.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        h5.addAttributeInfo(new AttributeInfo("dir", true));
        h5.addAttributeInfo(new AttributeInfo("id", true));
        h5.addAttributeInfo(new AttributeInfo("lang", true));
        h5.addAttributeInfo(new AttributeInfo("style", true));
        h5.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(h5);
        addTagInfo(h5);

        TagInfo h6 = new TagInfo("h6", true);
        h6.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        h6.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        h6.addAttributeInfo(new AttributeInfo("dir", true));
        h6.addAttributeInfo(new AttributeInfo("id", true));
        h6.addAttributeInfo(new AttributeInfo("lang", true));
        h6.addAttributeInfo(new AttributeInfo("style", true));
        h6.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(h6);

        TagInfo head = new TagInfo("head", true);
        head.addAttributeInfo(new AttributeInfo("profile", true));
        head.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        head.addAttributeInfo(new AttributeInfo("dir", true));
        head.addAttributeInfo(new AttributeInfo("id", true));
        head.addAttributeInfo(new AttributeInfo("lang", true));
        head.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(head);

        TagInfo hr = new TagInfo("hr", false, true);
        hr.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        hr.addAttributeInfo(new AttributeInfo("color", true));
        hr.addAttributeInfo(new AttributeInfo("nochade", false));
        hr.addAttributeInfo(new AttributeInfo("size", true));
        hr.addAttributeInfo(new AttributeInfo("width", true));
        hr.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        hr.addAttributeInfo(new AttributeInfo("dir", true));
        hr.addAttributeInfo(new AttributeInfo("id", true));
        hr.addAttributeInfo(new AttributeInfo("lang", true));
        hr.addAttributeInfo(new AttributeInfo("language", true));
        hr.addAttributeInfo(new AttributeInfo("style", true));
        hr.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(hr);
        addTagInfo(hr);

        TagInfo html = new TagInfo("html", true);
        html.addAttributeInfo(new AttributeInfo("version", true));
        html.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        html.addAttributeInfo(new AttributeInfo("dir", true));
        html.addAttributeInfo(new AttributeInfo("id", true));
        html.addAttributeInfo(new AttributeInfo("lang", true));
        html.addAttributeInfo(new AttributeInfo("language", true));
        html.addAttributeInfo(new AttributeInfo("style", true));
        html.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(html);

        TagInfo i = new TagInfo("i", true);
        i.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        i.addAttributeInfo(new AttributeInfo("dir", true));
        i.addAttributeInfo(new AttributeInfo("id", true));
        i.addAttributeInfo(new AttributeInfo("lang", true));
        i.addAttributeInfo(new AttributeInfo("style", true));
        i.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(i);
        addTagInfo(i);

        TagInfo iframe = new TagInfo("iframe", true);
        iframe.addAttributeInfo(new AttributeInfo("src", true,
            AttributeInfo.AttributeTypeOptions.FILE));
        iframe.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        iframe.addAttributeInfo(new AttributeInfo("height", true));
        iframe.addAttributeInfo(new AttributeInfo("width", true));
        iframe.addAttributeInfo(new AttributeInfo("hspace", true));
        iframe.addAttributeInfo(new AttributeInfo("vspace", true));
        iframe.addAttributeInfo(new AttributeInfo("marginheight", true));
        iframe.addAttributeInfo(new AttributeInfo("marginwidth", true));
        iframe.addAttributeInfo(new AttributeInfo("border", true));
        iframe.addAttributeInfo(new AttributeInfo("bordercolor", true));
        iframe.addAttributeInfo(new AttributeInfo("frameborder", true));
        iframe.addAttributeInfo(new AttributeInfo("framespacing", true));
        iframe.addAttributeInfo(new AttributeInfo("scrolling", true));
        iframe.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        iframe.addAttributeInfo(new AttributeInfo("dir", true));
        iframe.addAttributeInfo(new AttributeInfo("id", true));
        iframe.addAttributeInfo(new AttributeInfo("lang", true));
        iframe.addAttributeInfo(new AttributeInfo("language", true));
        iframe.addAttributeInfo(new AttributeInfo("name", true));
        iframe.addAttributeInfo(new AttributeInfo("style", true));
        iframe.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(iframe);

        TagInfo img = new TagInfo("img", false, true);
        img.addAttributeInfo(new AttributeInfo("src", true,
            AttributeInfo.AttributeTypeOptions.FILE));
        img.addAttributeInfo(new AttributeInfo("alt", true));
        img.addAttributeInfo(new AttributeInfo("height", true));
        img.addAttributeInfo(new AttributeInfo("width", true));
        img.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        img.addAttributeInfo(new AttributeInfo("border", true));
        img.addAttributeInfo(new AttributeInfo("galleryimg", true));
        img.addAttributeInfo(new AttributeInfo("hspace", true));
        img.addAttributeInfo(new AttributeInfo("lowsrc", true));
        img.addAttributeInfo(new AttributeInfo("suppress", true));
        img.addAttributeInfo(new AttributeInfo("vspace", true));
        img.addAttributeInfo(new AttributeInfo("ismap", false));
        img.addAttributeInfo(new AttributeInfo("usemap", true));
        img.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        img.addAttributeInfo(new AttributeInfo("dir", true));
        img.addAttributeInfo(new AttributeInfo("id", true));
        img.addAttributeInfo(new AttributeInfo("lang", true));
        img.addAttributeInfo(new AttributeInfo("language", true));
        img.addAttributeInfo(new AttributeInfo("name", true));
        img.addAttributeInfo(new AttributeInfo("style", true));
        img.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(img);
        addTagInfo(img);

        TagInfo input = new TagInfo("input", false, true);
        input.addAttributeInfo(new AttributeInfo("type", true,
            AttributeInfo.AttributeTypeOptions.INPUT_TYPE));
        input.addAttributeInfo(new AttributeInfo("value", true));
        input.addAttributeInfo(new AttributeInfo("size", true));
        input.addAttributeInfo(new AttributeInfo("maxlength", true));
        input.addAttributeInfo(new AttributeInfo("disabled", false));
        input.addAttributeInfo(new AttributeInfo("readonly", false));
        input.addAttributeInfo(new AttributeInfo("checked", false));
        input.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        input.addAttributeInfo(new AttributeInfo("alt", true));
        input.addAttributeInfo(new AttributeInfo("border", true));
        input.addAttributeInfo(new AttributeInfo("dynsrc", true));
        input.addAttributeInfo(new AttributeInfo("height", true));
        input.addAttributeInfo(new AttributeInfo("ismap", false));
        input.addAttributeInfo(new AttributeInfo("lowsrc", true));
        input.addAttributeInfo(new AttributeInfo("src", true));
        input.addAttributeInfo(new AttributeInfo("width", true));
        input.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        input.addAttributeInfo(new AttributeInfo("dir", true));
        input.addAttributeInfo(new AttributeInfo("id", true));
        input.addAttributeInfo(new AttributeInfo("lang", true));
        input.addAttributeInfo(new AttributeInfo("language", true));
        input.addAttributeInfo(new AttributeInfo("name", true));
        input.addAttributeInfo(new AttributeInfo("style", true));
        input.addAttributeInfo(new AttributeInfo("title", true));
        input.addAttributeInfo(new AttributeInfo("onfocus", true));
        input.addAttributeInfo(new AttributeInfo("onblur", true));
        input.addAttributeInfo(new AttributeInfo("onselect", true));
        input.addAttributeInfo(new AttributeInfo("onchange", true));
        addEventAttr(input);
        addTagInfo(input);

        TagInfo ins = new TagInfo("ins", true);
        ins.addAttributeInfo(new AttributeInfo("cite", true));
        ins.addAttributeInfo(new AttributeInfo("datetime", true));
        ins.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        ins.addAttributeInfo(new AttributeInfo("dir", true));
        ins.addAttributeInfo(new AttributeInfo("id", true));
        ins.addAttributeInfo(new AttributeInfo("lang", true));
        ins.addAttributeInfo(new AttributeInfo("style", true));
        ins.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(ins);
        addTagInfo(ins);

        TagInfo isindex = new TagInfo("isindex", false, true);
        isindex.addAttributeInfo(new AttributeInfo("action", true));
        isindex.addAttributeInfo(new AttributeInfo("prompt", true));
        isindex.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        isindex.addAttributeInfo(new AttributeInfo("dir", true));
        isindex.addAttributeInfo(new AttributeInfo("id", true));
        isindex.addAttributeInfo(new AttributeInfo("lang", true));
        isindex.addAttributeInfo(new AttributeInfo("style", true));
        isindex.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(isindex);

        TagInfo kbd = new TagInfo("kbd", true);
        kbd.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        kbd.addAttributeInfo(new AttributeInfo("dir", true));
        kbd.addAttributeInfo(new AttributeInfo("id", true));
        kbd.addAttributeInfo(new AttributeInfo("lang", true));
        kbd.addAttributeInfo(new AttributeInfo("style", true));
        kbd.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(kbd);
        addTagInfo(kbd);

        TagInfo keygen = new TagInfo("keygen", false);
        keygen.addAttributeInfo(new AttributeInfo("name", true));
        keygen.addAttributeInfo(new AttributeInfo("challenge", true));
        addTagInfo(keygen);

        TagInfo label = new TagInfo("label", true);
        label.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        label.addAttributeInfo(new AttributeInfo("dir", true));
        label.addAttributeInfo(new AttributeInfo("id", true));
        label.addAttributeInfo(new AttributeInfo("lang", true));
        label.addAttributeInfo(new AttributeInfo("style", true));
        label.addAttributeInfo(new AttributeInfo("title", true));
        label.addAttributeInfo(new AttributeInfo("onfocus", true));
        label.addAttributeInfo(new AttributeInfo("onblur", true));
        addEventAttr(label);
        addTagInfo(label);

        TagInfo legend = new TagInfo("legend", true);
        legend.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        legend.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        legend.addAttributeInfo(new AttributeInfo("dir", true));
        legend.addAttributeInfo(new AttributeInfo("id", true));
        legend.addAttributeInfo(new AttributeInfo("lang", true));
        legend.addAttributeInfo(new AttributeInfo("style", true));
        legend.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(legend);
        addTagInfo(legend);

        TagInfo li = new TagInfo("li", true);
        li.addAttributeInfo(new AttributeInfo("type", true));
        li.addAttributeInfo(new AttributeInfo("value", true));
        li.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        li.addAttributeInfo(new AttributeInfo("dir", true));
        li.addAttributeInfo(new AttributeInfo("id", true));
        li.addAttributeInfo(new AttributeInfo("lang", true));
        li.addAttributeInfo(new AttributeInfo("style", true));
        li.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(li);
        addTagInfo(li);

        TagInfo link = new TagInfo("link", false, true);
        link.addAttributeInfo(new AttributeInfo("rel", true));
        link.addAttributeInfo(new AttributeInfo("href", true,
            AttributeInfo.AttributeTypeOptions.FILE));
        link.addAttributeInfo(new AttributeInfo("src", true,
            AttributeInfo.AttributeTypeOptions.FILE));
        link.addAttributeInfo(new AttributeInfo("charset", true));
        link.addAttributeInfo(new AttributeInfo("disabled", false));
        link.addAttributeInfo(new AttributeInfo("hreflang", true));
        link.addAttributeInfo(new AttributeInfo("media", true));
        link.addAttributeInfo(new AttributeInfo("methods", true));
        link.addAttributeInfo(new AttributeInfo("rev", true));
        link.addAttributeInfo(new AttributeInfo("target", true));
        link.addAttributeInfo(new AttributeInfo("type", true));
        link.addAttributeInfo(new AttributeInfo("urn", true));
        link.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        link.addAttributeInfo(new AttributeInfo("dir", true));
        link.addAttributeInfo(new AttributeInfo("id", true));
        link.addAttributeInfo(new AttributeInfo("lang", true));
        link.addAttributeInfo(new AttributeInfo("language", true));
        link.addAttributeInfo(new AttributeInfo("style", true));
        link.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(link);
        addTagInfo(link);

        TagInfo map = new TagInfo("map", true);
        map.addAttributeInfo(new AttributeInfo("name", true));
        map.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        map.addAttributeInfo(new AttributeInfo("dir", true));
        map.addAttributeInfo(new AttributeInfo("id", true));
        map.addAttributeInfo(new AttributeInfo("lang", true));
        map.addAttributeInfo(new AttributeInfo("language", true));
        map.addAttributeInfo(new AttributeInfo("name", true));
        map.addAttributeInfo(new AttributeInfo("style", true));
        map.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(map);
        addTagInfo(map);

        TagInfo marquee = new TagInfo("marquee", true);
        marquee.addAttributeInfo(new AttributeInfo("behavior", true));
        marquee.addAttributeInfo(new AttributeInfo("bgcolor", true));
        marquee.addAttributeInfo(new AttributeInfo("direction", true));
        marquee.addAttributeInfo(new AttributeInfo("height", true));
        marquee.addAttributeInfo(new AttributeInfo("hspace", true));
        marquee.addAttributeInfo(new AttributeInfo("loop", true));
        marquee.addAttributeInfo(new AttributeInfo("scrollamount", true));
        marquee.addAttributeInfo(new AttributeInfo("scrolldelay", true));
        marquee.addAttributeInfo(new AttributeInfo("truespeed", false));
        marquee.addAttributeInfo(new AttributeInfo("vspace", true));
        marquee.addAttributeInfo(new AttributeInfo("width", true));
        marquee.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        marquee.addAttributeInfo(new AttributeInfo("dir", true));
        marquee.addAttributeInfo(new AttributeInfo("id", true));
        marquee.addAttributeInfo(new AttributeInfo("lang", true));
        marquee.addAttributeInfo(new AttributeInfo("style", true));
        marquee.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(marquee);
        addTagInfo(marquee);

        TagInfo menu = new TagInfo("menu", true);
        menu.addAttributeInfo(new AttributeInfo("compact", false));
        menu.addAttributeInfo(new AttributeInfo("type", true));
        menu.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        menu.addAttributeInfo(new AttributeInfo("dir", true));
        menu.addAttributeInfo(new AttributeInfo("id", true));
        menu.addAttributeInfo(new AttributeInfo("lang", true));
        menu.addAttributeInfo(new AttributeInfo("style", true));
        menu.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(menu);
        addTagInfo(menu);

        TagInfo meta = new TagInfo("meta", false, true);
        meta.addAttributeInfo(new AttributeInfo("name", true));
        meta.addAttributeInfo(new AttributeInfo("http-equiv", true));
        meta.addAttributeInfo(new AttributeInfo("content", true));
        meta.addAttributeInfo(new AttributeInfo("scheme", true));
        meta.addAttributeInfo(new AttributeInfo("dir", true));
        meta.addAttributeInfo(new AttributeInfo("lang", true));
        meta.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(meta);

        TagInfo nobr = new TagInfo("nobr", true);
        nobr.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        nobr.addAttributeInfo(new AttributeInfo("dir", true));
        nobr.addAttributeInfo(new AttributeInfo("id", true));
        nobr.addAttributeInfo(new AttributeInfo("lang", true));
        nobr.addAttributeInfo(new AttributeInfo("style", true));
        nobr.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(nobr);

        TagInfo noembed = new TagInfo("noembed", true);
        addTagInfo(noembed);

        TagInfo noframes = new TagInfo("noframes", true);
        noframes.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        noframes.addAttributeInfo(new AttributeInfo("dir", true));
        noframes.addAttributeInfo(new AttributeInfo("id", true));
        noframes.addAttributeInfo(new AttributeInfo("lang", true));
        noframes.addAttributeInfo(new AttributeInfo("style", true));
        noframes.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(noframes);

        TagInfo noscript = new TagInfo("noscript", true);
        noscript.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        noscript.addAttributeInfo(new AttributeInfo("dir", true));
        noscript.addAttributeInfo(new AttributeInfo("id", true));
        noscript.addAttributeInfo(new AttributeInfo("lang", true));
        noscript.addAttributeInfo(new AttributeInfo("style", true));
        noscript.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(noscript);

        TagInfo object = new TagInfo("object", true);
        object.addAttributeInfo(new AttributeInfo("alt", true));
        object.addAttributeInfo(new AttributeInfo("archive", true));
        object.addAttributeInfo(new AttributeInfo("border", true));
        object.addAttributeInfo(new AttributeInfo("classid", true));
        object.addAttributeInfo(new AttributeInfo("code", true));
        object.addAttributeInfo(new AttributeInfo("codebase", true));
        object.addAttributeInfo(new AttributeInfo("codetype", true));
        object.addAttributeInfo(new AttributeInfo("data", true));
        object.addAttributeInfo(new AttributeInfo("declare", false));
        object.addAttributeInfo(new AttributeInfo("name", true));
        object.addAttributeInfo(new AttributeInfo("standby", true));
        object.addAttributeInfo(new AttributeInfo("type", true));
        object.addAttributeInfo(new AttributeInfo("usemap", true));
        object.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        object.addAttributeInfo(new AttributeInfo("width", true));
        object.addAttributeInfo(new AttributeInfo("height", true));
        object.addAttributeInfo(new AttributeInfo("hspace", true));
        object.addAttributeInfo(new AttributeInfo("vspace", true));
        object.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        object.addAttributeInfo(new AttributeInfo("dir", true));
        object.addAttributeInfo(new AttributeInfo("id", true));
        object.addAttributeInfo(new AttributeInfo("lang", true));
        object.addAttributeInfo(new AttributeInfo("language", true));
        object.addAttributeInfo(new AttributeInfo("style", true));
        object.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(object);
        addTagInfo(object);

        TagInfo ol = new TagInfo("ol", true);
        ol.addAttributeInfo(new AttributeInfo("compact", false));
        ol.addAttributeInfo(new AttributeInfo("start", true));
        ol.addAttributeInfo(new AttributeInfo("type", true));
        ol.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        ol.addAttributeInfo(new AttributeInfo("dir", true));
        ol.addAttributeInfo(new AttributeInfo("id", true));
        ol.addAttributeInfo(new AttributeInfo("lang", true));
        ol.addAttributeInfo(new AttributeInfo("style", true));
        ol.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(ol);
        addTagInfo(ol);

        TagInfo optgroup = new TagInfo("optgroup", true);
        optgroup.addAttributeInfo(new AttributeInfo("disabled", false));
        optgroup.addAttributeInfo(new AttributeInfo("label", false));
        optgroup.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        optgroup.addAttributeInfo(new AttributeInfo("dir", true));
        optgroup.addAttributeInfo(new AttributeInfo("id", true));
        optgroup.addAttributeInfo(new AttributeInfo("lang", true));
        optgroup.addAttributeInfo(new AttributeInfo("language", true));
        optgroup.addAttributeInfo(new AttributeInfo("style", true));
        optgroup.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(optgroup);
        addTagInfo(optgroup);

        TagInfo option = new TagInfo("option", true);
        option.addAttributeInfo(new AttributeInfo("disabled", false));
        option.addAttributeInfo(new AttributeInfo("label", false));
        option.addAttributeInfo(new AttributeInfo("selected", false));
        option.addAttributeInfo(new AttributeInfo("value", true));
        option.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        option.addAttributeInfo(new AttributeInfo("dir", true));
        option.addAttributeInfo(new AttributeInfo("id", true));
        option.addAttributeInfo(new AttributeInfo("lang", true));
        option.addAttributeInfo(new AttributeInfo("language", true));
        option.addAttributeInfo(new AttributeInfo("style", true));
        option.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(option);
        addTagInfo(option);

        TagInfo p = new TagInfo("p", true);
        p.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        p.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        p.addAttributeInfo(new AttributeInfo("dir", true));
        p.addAttributeInfo(new AttributeInfo("id", true));
        p.addAttributeInfo(new AttributeInfo("lang", true));
        p.addAttributeInfo(new AttributeInfo("style", true));
        p.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(p);
        addTagInfo(p);

        TagInfo param = new TagInfo("param", false, true);
        param.addAttributeInfo(new AttributeInfo("name", true));
        param.addAttributeInfo(new AttributeInfo("value", true));
        param.addAttributeInfo(new AttributeInfo("valuetype", true));
        param.addAttributeInfo(new AttributeInfo("type", true));
        param.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        param.addAttributeInfo(new AttributeInfo("dir", true));
        param.addAttributeInfo(new AttributeInfo("id", true));
        param.addAttributeInfo(new AttributeInfo("lang", true));
        param.addAttributeInfo(new AttributeInfo("language", true));
        param.addAttributeInfo(new AttributeInfo("style", true));
        param.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(param);

        TagInfo pre = new TagInfo("pre", true);
        pre.addAttributeInfo(new AttributeInfo("cols", true));
        pre.addAttributeInfo(new AttributeInfo("width", true));
        pre.addAttributeInfo(new AttributeInfo("wrap", false));
        pre.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        pre.addAttributeInfo(new AttributeInfo("dir", true));
        pre.addAttributeInfo(new AttributeInfo("id", true));
        pre.addAttributeInfo(new AttributeInfo("lang", true));
        pre.addAttributeInfo(new AttributeInfo("style", true));
        pre.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(pre);
        addTagInfo(pre);

        TagInfo q = new TagInfo("q", true);
        q.addAttributeInfo(new AttributeInfo("cite", true));
        q.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        q.addAttributeInfo(new AttributeInfo("dir", true));
        q.addAttributeInfo(new AttributeInfo("id", true));
        q.addAttributeInfo(new AttributeInfo("lang", true));
        q.addAttributeInfo(new AttributeInfo("style", true));
        q.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(q);
        addTagInfo(q);

        TagInfo rb = new TagInfo("rb", true);
        rb.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        rb.addAttributeInfo(new AttributeInfo("dir", true));
        rb.addAttributeInfo(new AttributeInfo("id", true));
        rb.addAttributeInfo(new AttributeInfo("lang", true));
        rb.addAttributeInfo(new AttributeInfo("language", true));
        rb.addAttributeInfo(new AttributeInfo("style", true));
        rb.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(rb);

        TagInfo rp = new TagInfo("rp", true);
        rp.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        rp.addAttributeInfo(new AttributeInfo("dir", true));
        rp.addAttributeInfo(new AttributeInfo("id", true));
        rp.addAttributeInfo(new AttributeInfo("lang", true));
        rp.addAttributeInfo(new AttributeInfo("language", true));
        rp.addAttributeInfo(new AttributeInfo("style", true));
        rp.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(rp);

        TagInfo rt = new TagInfo("rt", true);
        rt.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        rt.addAttributeInfo(new AttributeInfo("dir", true));
        rt.addAttributeInfo(new AttributeInfo("id", true));
        rt.addAttributeInfo(new AttributeInfo("lang", true));
        rt.addAttributeInfo(new AttributeInfo("style", true));
        rt.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(rt);

        TagInfo ruby = new TagInfo("ruby", true);
        ruby.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        ruby.addAttributeInfo(new AttributeInfo("dir", true));
        ruby.addAttributeInfo(new AttributeInfo("id", true));
        ruby.addAttributeInfo(new AttributeInfo("lang", true));
        ruby.addAttributeInfo(new AttributeInfo("style", true));
        ruby.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(ruby);

        TagInfo s = new TagInfo("s", true);
        s.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        s.addAttributeInfo(new AttributeInfo("dir", true));
        s.addAttributeInfo(new AttributeInfo("id", true));
        s.addAttributeInfo(new AttributeInfo("lang", true));
        s.addAttributeInfo(new AttributeInfo("style", true));
        s.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(s);
        addTagInfo(s);

        TagInfo samp = new TagInfo("samp", true);
        samp.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        samp.addAttributeInfo(new AttributeInfo("dir", true));
        samp.addAttributeInfo(new AttributeInfo("id", true));
        samp.addAttributeInfo(new AttributeInfo("lang", true));
        samp.addAttributeInfo(new AttributeInfo("style", true));
        samp.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(samp);
        addTagInfo(samp);

        TagInfo script = new TagInfo("script", true);
        script.addAttributeInfo(new AttributeInfo("charset", true));
        script.addAttributeInfo(new AttributeInfo("defer", false));
        script.addAttributeInfo(new AttributeInfo("event", true));
        script.addAttributeInfo(new AttributeInfo("for", true));
        script.addAttributeInfo(new AttributeInfo("src", true,
            AttributeInfo.AttributeTypeOptions.FILE));
        script.addAttributeInfo(new AttributeInfo("type", true));
        script.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        script.addAttributeInfo(new AttributeInfo("id", true));
        script.addAttributeInfo(new AttributeInfo("language", true));
        script.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(script);

        TagInfo select = new TagInfo("select", true);
        select.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        select.addAttributeInfo(new AttributeInfo("disabled", false));
        select.addAttributeInfo(new AttributeInfo("multiple", false));
        select.addAttributeInfo(new AttributeInfo("size", true));
        select.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        select.addAttributeInfo(new AttributeInfo("dir", true));
        select.addAttributeInfo(new AttributeInfo("id", true));
        select.addAttributeInfo(new AttributeInfo("lang", true));
        select.addAttributeInfo(new AttributeInfo("language", true));
        select.addAttributeInfo(new AttributeInfo("name", true));
        select.addAttributeInfo(new AttributeInfo("style", true));
        select.addAttributeInfo(new AttributeInfo("title", true));
        select.addAttributeInfo(new AttributeInfo("onfocus", true));
        select.addAttributeInfo(new AttributeInfo("onblur", true));
        addEventAttr(select);
        addTagInfo(select);

        TagInfo small = new TagInfo("small", true);
        small.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        small.addAttributeInfo(new AttributeInfo("dir", true));
        small.addAttributeInfo(new AttributeInfo("id", true));
        small.addAttributeInfo(new AttributeInfo("lang", true));
        small.addAttributeInfo(new AttributeInfo("style", true));
        small.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(small);
        addTagInfo(small);

        TagInfo span = new TagInfo("span", true);
        span.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        span.addAttributeInfo(new AttributeInfo("dir", true));
        span.addAttributeInfo(new AttributeInfo("id", true));
        span.addAttributeInfo(new AttributeInfo("lang", true));
        span.addAttributeInfo(new AttributeInfo("style", true));
        span.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(span);
        addTagInfo(span);

        TagInfo strike = new TagInfo("strike", true);
        strike.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        strike.addAttributeInfo(new AttributeInfo("dir", true));
        strike.addAttributeInfo(new AttributeInfo("id", true));
        strike.addAttributeInfo(new AttributeInfo("lang", true));
        strike.addAttributeInfo(new AttributeInfo("style", true));
        strike.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(strike);
        addTagInfo(strike);

        TagInfo strong = new TagInfo("strong", true);
        strong.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        strong.addAttributeInfo(new AttributeInfo("dir", true));
        strong.addAttributeInfo(new AttributeInfo("id", true));
        strong.addAttributeInfo(new AttributeInfo("lang", true));
        strong.addAttributeInfo(new AttributeInfo("style", true));
        strong.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(strong);
        addTagInfo(strong);

        TagInfo style = new TagInfo("style", true);
        style.addAttributeInfo(new AttributeInfo("disabled", true));
        style.addAttributeInfo(new AttributeInfo("media", true));
        style.addAttributeInfo(new AttributeInfo("type", true));
        style.addAttributeInfo(new AttributeInfo("dir", true));
        style.addAttributeInfo(new AttributeInfo("lang", true));
        style.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(style);
        addTagInfo(style);

        TagInfo sub = new TagInfo("sub", true);
        sub.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        sub.addAttributeInfo(new AttributeInfo("dir", true));
        sub.addAttributeInfo(new AttributeInfo("id", true));
        sub.addAttributeInfo(new AttributeInfo("lang", true));
        sub.addAttributeInfo(new AttributeInfo("style", true));
        sub.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(sub);
        addTagInfo(sub);

        TagInfo sup = new TagInfo("sup", true);
        sup.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        sup.addAttributeInfo(new AttributeInfo("dir", true));
        sup.addAttributeInfo(new AttributeInfo("id", true));
        sup.addAttributeInfo(new AttributeInfo("lang", true));
        sup.addAttributeInfo(new AttributeInfo("style", true));
        sup.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(sup);
        addTagInfo(sup);

        TagInfo table = new TagInfo("table", true);
        table.addAttributeInfo(new AttributeInfo("border", true));
        table.addAttributeInfo(new AttributeInfo("bordercolor", true));
        table.addAttributeInfo(new AttributeInfo("bordercolordark", true));
        table.addAttributeInfo(new AttributeInfo("bordercolorlight", true));
        table.addAttributeInfo(new AttributeInfo("frame", true));
        table.addAttributeInfo(new AttributeInfo("rules", true));
        table.addAttributeInfo(new AttributeInfo("background", true));
        table.addAttributeInfo(new AttributeInfo("bgcolor", true));
        table.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        table.addAttributeInfo(new AttributeInfo("cellpadding", true));
        table.addAttributeInfo(new AttributeInfo("cellspacing", true));
        table.addAttributeInfo(new AttributeInfo("height", true));
        table.addAttributeInfo(new AttributeInfo("hspace", true));
        table.addAttributeInfo(new AttributeInfo("vspace", true));
        table.addAttributeInfo(new AttributeInfo("width", true));
        table.addAttributeInfo(new AttributeInfo("cols", true));
        table.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        table.addAttributeInfo(new AttributeInfo("dir", true));
        table.addAttributeInfo(new AttributeInfo("id", true));
        table.addAttributeInfo(new AttributeInfo("lang", true));
        table.addAttributeInfo(new AttributeInfo("language", true));
        table.addAttributeInfo(new AttributeInfo("style", true));
        table.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(table);
        addTagInfo(table);

        TagInfo tbody = new TagInfo("tbody", true);
        tbody.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        tbody.addAttributeInfo(new AttributeInfo("bgcolor", true));
        tbody.addAttributeInfo(new AttributeInfo("char", true));
        tbody.addAttributeInfo(new AttributeInfo("charoff", true));
        tbody.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        tbody.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        tbody.addAttributeInfo(new AttributeInfo("dir", true));
        tbody.addAttributeInfo(new AttributeInfo("id", true));
        tbody.addAttributeInfo(new AttributeInfo("lang", true));
        tbody.addAttributeInfo(new AttributeInfo("language", true));
        tbody.addAttributeInfo(new AttributeInfo("style", true));
        tbody.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(tbody);
        addTagInfo(tbody);

        TagInfo td = new TagInfo("td", true);
        td.addAttributeInfo(new AttributeInfo("bordercolor", true));
        td.addAttributeInfo(new AttributeInfo("bordercolordark", true));
        td.addAttributeInfo(new AttributeInfo("bordercolorlight", true));
        td.addAttributeInfo(new AttributeInfo("background", true));
        td.addAttributeInfo(new AttributeInfo("bgcolor", true));
        td.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        td.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        td.addAttributeInfo(new AttributeInfo("height", true));
        td.addAttributeInfo(new AttributeInfo("width", true));
        td.addAttributeInfo(new AttributeInfo("nowrap", false));
        td.addAttributeInfo(new AttributeInfo("char", true));
        td.addAttributeInfo(new AttributeInfo("charoff", true));
        td.addAttributeInfo(new AttributeInfo("colspan", true));
        td.addAttributeInfo(new AttributeInfo("rowspan", true));
        td.addAttributeInfo(new AttributeInfo("abbr", true));
        td.addAttributeInfo(new AttributeInfo("axis", true));
        td.addAttributeInfo(new AttributeInfo("headers", true));
        td.addAttributeInfo(new AttributeInfo("scope", true));
        td.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        td.addAttributeInfo(new AttributeInfo("dir", true));
        td.addAttributeInfo(new AttributeInfo("id", true));
        td.addAttributeInfo(new AttributeInfo("lang", true));
        td.addAttributeInfo(new AttributeInfo("language", true));
        td.addAttributeInfo(new AttributeInfo("style", true));
        td.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(td);
        addTagInfo(td);

        TagInfo textarea = new TagInfo("textarea", true);
        textarea.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        textarea.addAttributeInfo(new AttributeInfo("cols", true));
        textarea.addAttributeInfo(new AttributeInfo("disabled", false));
        textarea.addAttributeInfo(new AttributeInfo("istyle", true));
        textarea.addAttributeInfo(new AttributeInfo("readonly", false));
        textarea.addAttributeInfo(new AttributeInfo("rows", true));
        textarea.addAttributeInfo(new AttributeInfo("wrap", true));
        textarea.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        textarea.addAttributeInfo(new AttributeInfo("dir", true));
        textarea.addAttributeInfo(new AttributeInfo("id", true));
        textarea.addAttributeInfo(new AttributeInfo("lang", true));
        textarea.addAttributeInfo(new AttributeInfo("name", true));
        textarea.addAttributeInfo(new AttributeInfo("style", true));
        textarea.addAttributeInfo(new AttributeInfo("title", true));
        textarea.addAttributeInfo(new AttributeInfo("onfocus", true));
        textarea.addAttributeInfo(new AttributeInfo("onblur", true));
        textarea.addAttributeInfo(new AttributeInfo("onselect", true));
        textarea.addAttributeInfo(new AttributeInfo("onchange", true));
        addEventAttr(textarea);
        addTagInfo(textarea);

        TagInfo tfoot = new TagInfo("tfoot", true);
        tfoot.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        tfoot.addAttributeInfo(new AttributeInfo("bgcolor", true));
        tfoot.addAttributeInfo(new AttributeInfo("char", true));
        tfoot.addAttributeInfo(new AttributeInfo("charoff", true));
        tfoot.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        tfoot.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        tfoot.addAttributeInfo(new AttributeInfo("dir", true));
        tfoot.addAttributeInfo(new AttributeInfo("id", true));
        tfoot.addAttributeInfo(new AttributeInfo("lang", true));
        tfoot.addAttributeInfo(new AttributeInfo("language", true));
        tfoot.addAttributeInfo(new AttributeInfo("style", true));
        tfoot.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(tfoot);
        addTagInfo(tfoot);

        TagInfo th = new TagInfo("th", true);
        th.addAttributeInfo(new AttributeInfo("bordercolor", true));
        th.addAttributeInfo(new AttributeInfo("bordercolordark", true));
        th.addAttributeInfo(new AttributeInfo("bordercolorlight", true));
        th.addAttributeInfo(new AttributeInfo("background", true));
        th.addAttributeInfo(new AttributeInfo("bgcolor", true));
        th.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        th.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        th.addAttributeInfo(new AttributeInfo("height", true));
        th.addAttributeInfo(new AttributeInfo("width", true));
        th.addAttributeInfo(new AttributeInfo("nowrap", false));
        th.addAttributeInfo(new AttributeInfo("char", true));
        th.addAttributeInfo(new AttributeInfo("charoff", true));
        th.addAttributeInfo(new AttributeInfo("colspan", true));
        th.addAttributeInfo(new AttributeInfo("rowspan", true));
        th.addAttributeInfo(new AttributeInfo("abbr", true));
        th.addAttributeInfo(new AttributeInfo("axis", true));
        th.addAttributeInfo(new AttributeInfo("headers", true));
        th.addAttributeInfo(new AttributeInfo("scope", true));
        th.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        th.addAttributeInfo(new AttributeInfo("dir", true));
        th.addAttributeInfo(new AttributeInfo("id", true));
        th.addAttributeInfo(new AttributeInfo("lang", true));
        th.addAttributeInfo(new AttributeInfo("language", true));
        th.addAttributeInfo(new AttributeInfo("style", true));
        th.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(th);
        addTagInfo(th);

        TagInfo thead = new TagInfo("thead", true);
        thead.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        thead.addAttributeInfo(new AttributeInfo("bgcolor", true));
        thead.addAttributeInfo(new AttributeInfo("char", true));
        thead.addAttributeInfo(new AttributeInfo("charoff", true));
        thead.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        thead.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        thead.addAttributeInfo(new AttributeInfo("dir", true));
        thead.addAttributeInfo(new AttributeInfo("id", true));
        thead.addAttributeInfo(new AttributeInfo("lang", true));
        thead.addAttributeInfo(new AttributeInfo("language", true));
        thead.addAttributeInfo(new AttributeInfo("style", true));
        thead.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(thead);
        addTagInfo(thead);

        TagInfo title = new TagInfo("title", true);
        title.addAttributeInfo(new AttributeInfo("dir", true));
        title.addAttributeInfo(new AttributeInfo("id", true));
        title.addAttributeInfo(new AttributeInfo("lang", true));
        title.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(title);

        TagInfo tr = new TagInfo("tr", true);
        tr.addAttributeInfo(new AttributeInfo("bordercolor", true));
        tr.addAttributeInfo(new AttributeInfo("bordercolordark", true));
        tr.addAttributeInfo(new AttributeInfo("bordercolorlight", true));
        tr.addAttributeInfo(new AttributeInfo("background", true));
        tr.addAttributeInfo(new AttributeInfo("bgcolor", true));
        tr.addAttributeInfo(new AttributeInfo("align", true,
            AttributeInfo.AttributeTypeOptions.ALIGN));
        tr.addAttributeInfo(new AttributeInfo("char", true));
        tr.addAttributeInfo(new AttributeInfo("charoff", true));
        tr.addAttributeInfo(new AttributeInfo("height", true));
        tr.addAttributeInfo(new AttributeInfo("valign", true,
            AttributeInfo.AttributeTypeOptions.VALIGN));
        tr.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        tr.addAttributeInfo(new AttributeInfo("dir", true));
        tr.addAttributeInfo(new AttributeInfo("id", true));
        tr.addAttributeInfo(new AttributeInfo("lang", true));
        tr.addAttributeInfo(new AttributeInfo("language", true));
        tr.addAttributeInfo(new AttributeInfo("style", true));
        tr.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(tr);
        addTagInfo(tr);

        TagInfo tt = new TagInfo("tt", true);
        tt.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        tt.addAttributeInfo(new AttributeInfo("dir", true));
        tt.addAttributeInfo(new AttributeInfo("id", true));
        tt.addAttributeInfo(new AttributeInfo("lang", true));
        tt.addAttributeInfo(new AttributeInfo("style", true));
        tt.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(tt);
        addTagInfo(tt);

        TagInfo u = new TagInfo("u", true);
        u.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        u.addAttributeInfo(new AttributeInfo("dir", true));
        u.addAttributeInfo(new AttributeInfo("id", true));
        u.addAttributeInfo(new AttributeInfo("lang", true));
        u.addAttributeInfo(new AttributeInfo("style", true));
        u.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(u);
        addTagInfo(u);

        TagInfo ul = new TagInfo("ul", true);
        ul.addAttributeInfo(new AttributeInfo("compact", false));
        ul.addAttributeInfo(new AttributeInfo("type", true));
        ul.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        ul.addAttributeInfo(new AttributeInfo("dir", true));
        ul.addAttributeInfo(new AttributeInfo("id", true));
        ul.addAttributeInfo(new AttributeInfo("lang", true));
        ul.addAttributeInfo(new AttributeInfo("style", true));
        ul.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(ul);
        addTagInfo(ul);

        TagInfo var = new TagInfo("var", true);
        var.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        var.addAttributeInfo(new AttributeInfo("dir", true));
        var.addAttributeInfo(new AttributeInfo("id", true));
        var.addAttributeInfo(new AttributeInfo("lang", true));
        var.addAttributeInfo(new AttributeInfo("style", true));
        var.addAttributeInfo(new AttributeInfo("title", true));
        addEventAttr(var);
        addTagInfo(var);

        TagInfo wbr = new TagInfo("wbr", false);
        wbr.addAttributeInfo(new AttributeInfo("class", true,
            AttributeInfo.AttributeTypeOptions.CSS));
        wbr.addAttributeInfo(new AttributeInfo("dir", true));
        wbr.addAttributeInfo(new AttributeInfo("id", true));
        wbr.addAttributeInfo(new AttributeInfo("lang", true));
        wbr.addAttributeInfo(new AttributeInfo("language", true));
        wbr.addAttributeInfo(new AttributeInfo("style", true));
        wbr.addAttributeInfo(new AttributeInfo("title", true));
        addTagInfo(wbr);
    }

}
