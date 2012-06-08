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
                xmlns:m="http://maven.apache.org/POM/4.0.0"
                exclude-result-prefixes="m"
                version="1.0">
  <xsl:param name="syncopeVersion"/>
  
  <xsl:template match="/m:project/m:parent/m:version">
    <version>
      <xsl:value-of select="$syncopeVersion"/>
    </version>
  </xsl:template>

  <xsl:template match="/m:project/m:properties/m:syncope.version">
    <syncope.version>
      <xsl:value-of select="$syncopeVersion"/>
    </syncope.version>
  </xsl:template>
  
  <xsl:template match="node()|@*|comment()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
