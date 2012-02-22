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

  <xsl:import href="userReportlet2fo.xsl"/>
 
  <xsl:param name="status"/>
  <xsl:param name="message"/>
  <xsl:param name="startDate"/>
  <xsl:param name="endDate"/>
  
  <xsl:template match="/">
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" font-family="Helvetica" font-size="10pt">
      
      <!-- defines the layout master -->
      <fo:layout-master-set>
	<fo:simple-page-master master-name="first" page-height="29.7cm" page-width="21cm" 
			       margin-top="1cm" margin-bottom="2cm" margin-left="2.5cm" margin-right="2.5cm">
	  <fo:region-body margin-top="1cm"/>
	  <fo:region-before extent="1cm"/>
	  <fo:region-after extent="1.5cm"/>
	</fo:simple-page-master>
      </fo:layout-master-set>

      <!-- starts actual layout -->
      <fo:page-sequence master-reference="first">
	
	<fo:flow flow-name="xsl-region-body">
	  <fo:block font-size="24pt" font-weight="bold" text-align="center" space-after="1cm">
	    Syncope Report - <xsl:value-of select="report/@name"/>
	  </fo:block>

	  <fo:table table-layout="fixed" border-width="0.5mm" border-style="solid" width="100%" space-after="1cm">
	    <fo:table-column column-width="proportional-column-width(1)"/>
	    <fo:table-column column-width="proportional-column-width(1)"/>
	    <fo:table-body>
	      <fo:table-row>
		<fo:table-cell>
		  <fo:block font-size="18pt" font-weight="bold">Report Name:</fo:block>
		</fo:table-cell>
		<fo:table-cell>
		  <fo:block font-size="18pt" font-weight="bold"><xsl:value-of select="report/@name"/></fo:block>
		</fo:table-cell>
	      </fo:table-row>
	      <fo:table-row>
		<fo:table-cell>
		  <fo:block font-size="18pt" font-weight="bold">Start Date:</fo:block>
		</fo:table-cell>
		<fo:table-cell>
		  <fo:block font-size="18pt" font-weight="bold"><xsl:value-of select="$startDate"/></fo:block>
		</fo:table-cell>
	      </fo:table-row>
	      <fo:table-row>
		<fo:table-cell>
		  <fo:block font-size="18pt" font-weight="bold">End Date:</fo:block>
		</fo:table-cell>
		<fo:table-cell>
		  <fo:block font-size="18pt" font-weight="bold"><xsl:value-of select="$endDate"/></fo:block>
		</fo:table-cell>
	      </fo:table-row>
	    </fo:table-body>
	  </fo:table>

	  <xsl:apply-templates/>
	</fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>

</xsl:stylesheet>