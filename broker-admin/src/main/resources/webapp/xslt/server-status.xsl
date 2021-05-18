<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 xmlns:a="http://aktin.org/ns/exchange"
>

<xsl:template match="/">
	<html><body>
			<table><tbody>
				<tr><th>Module</th><th>Version</th></tr>
				<xsl:apply-templates select="/a:broker-status/a:node-software/a:module"/>
			</tbody></table>
	</body></html>
</xsl:template>


<xsl:template match="a:module">
	<tr>
		<td><xsl:value-of select="@id"/></td>
		<td align="right"><xsl:value-of select="version/text()"/></td>
	</tr>
</xsl:template>


</xsl:stylesheet> 