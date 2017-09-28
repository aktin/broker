<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
	<html><body>
			<table border="1"><tbody>
				<xsl:apply-templates select="/import-statistics/*[not(self::last-errors)]"/>
			</tbody></table>
			<xsl:apply-templates select="/import-statistics/last-errors"/>			
	</body></html>
</xsl:template>


<xsl:template match="/import-statistics/*">
	<tr>
		<td><xsl:value-of select="local-name(.)"/></td>
		<td align="right"><xsl:value-of select="."/></td>
	</tr>
</xsl:template>

<xsl:template match="/import-statistics/last-errors">
	<dl>
		<xsl:apply-templates/>
	</dl>
</xsl:template>

<xsl:template match="/import-statistics/last-errors/error">
	<dt>Error <xsl:value-of select="@timestamp"/>
		<xsl:if test="@repeats">, <strong>repeated=<xsl:value-of select="@repeats"/></strong></xsl:if>
	</dt>
	<!-- TODO repeats -->
	<dd><pre><xsl:value-of select="."/></pre></dd>
</xsl:template>


</xsl:stylesheet> 