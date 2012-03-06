<?xml version="1.0" encoding="UTF-8"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version="1.0">

    <xsl:template match="reportlet[@class='org.syncope.core.report.StaticReportlet']">

        <fo:block font-size="14pt" font-weight="bold" space-after="0.5cm">Reportlet: 
            <xsl:value-of select="@name"/>
        </fo:block>

        <xsl:if test="string-length(string/text()) &gt; 0">
            <fo:block font-size="11pt">String value: 
                <xsl:value-of select="string/text()"/>
            </fo:block>
        </xsl:if>

        <xsl:if test="string-length(long/text()) &gt; 0">
            <fo:block font-size="11pt">Long value: 
                <xsl:value-of select="long/text()"/>
            </fo:block>
        </xsl:if>

        <xsl:if test="string-length(double/text()) &gt; 0">
            <fo:block font-size="11pt">Double value: 
                <xsl:value-of select="double/text()"/>
            </fo:block>
        </xsl:if>

        <xsl:if test="string-length(date/text()) &gt; 0">
            <fo:block font-size="11pt">Date value: 
                <xsl:value-of select="date/text()"/>
            </fo:block>
        </xsl:if>

        <xsl:if test="string-length(enum/text()) &gt; 0">
            <fo:block font-size="11pt">Enum value: 
                <xsl:value-of select="enum/text()"/>
            </fo:block>
        </xsl:if>

        <xsl:if test="string-length(list) &gt; 0">
            <fo:block font-size="11pt">List values:</fo:block>
            
            <fo:list-block provisional-label-separation="4mm" provisional-distance-between-starts="2mm">
                <xsl:for-each select="list/string">
                    <xsl:if test="string-length(string/text()) &gt; 0">
                        <fo:list-item>
                            <fo:list-item-label end-indent="label-end()">
                                <fo:block>&#x2022;</fo:block>
                            </fo:list-item-label>
                            <fo:list-item-body start-indent="body-start()">
                                <fo:block>
                                    <xsl:value-of select="text()"/>
                                </fo:block>
                            </fo:list-item-body>
                        </fo:list-item>
                    </xsl:if>
                </xsl:for-each>
            </fo:list-block>
        </xsl:if>
        
    </xsl:template>
</xsl:stylesheet>