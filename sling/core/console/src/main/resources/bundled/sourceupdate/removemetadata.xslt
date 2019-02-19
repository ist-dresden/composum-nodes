<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:jcr="http://www.jcp.org/jcr/1.0">

    <!-- Remove various metadata attributes. -->
    <xsl:template match="@jcr:uuid"/>
    <xsl:template match="@jcr:lastModified"/>
    <xsl:template match="@jcr:lastModifiedBy"/>
    <xsl:template match="@jcr:created"/>
    <xsl:template match="@jcr:createdBy"/>
    <xsl:template match="@jcr:isCheckedOut"/>
    <xsl:template match="@jcr:baseVersion"/>
    <xsl:template match="@jcr:versionHistory"/>
    <xsl:template match="@jcr:predecessors"/>
    <xsl:template match="@jcr:mergeFailed"/>
    <xsl:template match="@jcr:mergeFailed"/>
    <xsl:template match="@jcr:configuration"/>

    <!-- Copy all nodes and attributes unless another rule indicates otherwise. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
