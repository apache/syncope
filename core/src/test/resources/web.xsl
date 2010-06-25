<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:javaee="http://java.sun.com/xml/ns/javaee"
                version="1.0">

    <xsl:output method="xml"/>

    <xsl:template match="javaee:resource-ref"/>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="@*" />
            <xsl:apply-templates />
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
