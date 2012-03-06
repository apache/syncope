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
                version="1.0">

    <xsl:template match="reportlet[@class='org.syncope.core.report.StaticReportlet']">
        <h2>Reportlet: 
            <xsl:value-of select="@name"/>
        </h2>
        
        <xsl:if test="string-length(string/text()) &gt; 0">
            <p>String value:                 
                <xsl:value-of select="string/text()"/>
            </p>
        </xsl:if>

        <xsl:if test="string-length(long/text()) &gt; 0">
            <p>Long value: 
                <xsl:value-of select="long/text()"/>
            </p>
        </xsl:if>

        <xsl:if test="string-length(double/text()) &gt; 0">
            <p>Double value: 
                <xsl:value-of select="double/text()"/>
            </p>
        </xsl:if>

        <xsl:if test="string-length(date/text()) &gt; 0">
            <p>Date value: 
                <xsl:value-of select="date/text()"/>
            </p>
        </xsl:if>

        <xsl:if test="string-length(enum/text()) &gt; 0">
            <p>Enum value: 
                <xsl:value-of select="enum/text()"/>
            </p>
        </xsl:if>

        <xsl:if test="string-length(list) &gt; 0">
            <p>List values:</p>
            
            <ul>
                <xsl:for-each select="list/string">
                    <xsl:if test="string-length(string/text()) &gt; 0">
                        <li>
                            <xsl:value-of select="text()"/>
                        </li>
                    </xsl:if>
                </xsl:for-each>
            </ul>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>