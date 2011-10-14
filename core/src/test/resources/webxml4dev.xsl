<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:javaee="http://java.sun.com/xml/ns/javaee"
                xmlns="http://java.sun.com/xml/ns/javaee"
                version="1.0">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>

    <xsl:template match="javaee:web-app">
        <web-app>
            <xsl:apply-templates select="@*"/>

            <listener>
                <listener-class>org.syncope.core.dev.init.H2ConsoleContextListener</listener-class>    
            </listener>
        
            <xsl:apply-templates/>
        </web-app>
    </xsl:template>


    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>
