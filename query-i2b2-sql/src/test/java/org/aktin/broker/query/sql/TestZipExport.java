package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.aktin.broker.query.io.TableWriter;
import org.aktin.broker.query.io.ZipArchiveWriter;
import org.junit.Assert;
import org.junit.Test;

public class TestZipExport {

	@Test
	public void writeReadZipArchive() throws IOException{
		Path temp = Files.createTempFile("queries", ".zip");
		System.out.println("Writing to "+temp.toString());
		// write ZIP file
		try( OutputStream out = Files.newOutputStream(temp) ){
			ZipArchiveWriter z = new ZipArchiveWriter(out, StandardCharsets.UTF_8);
			TableWriter t = z.exportTable("patients");
			t.header("id","sex");
			t.row("1","m");
			t.row("2","w");
			t.close();
			t = z.exportTable("visits");
			t.header("id","start", "end");
			t.row("1","2000-01-01","2000-01-01");
			t.row("2","2000-01-01","2000-01-01");
			t.close();
			z.close();
		}
		// read back ZIP file
		ZipFile z = new ZipFile(temp.toFile());
		Enumeration<? extends ZipEntry> e = z.entries();
		ZipEntry ze = e.nextElement();
		Assert.assertEquals("patients.txt", ze.getName());
		Assert.assertEquals(12+3*System.lineSeparator().length(), ze.getSize());
		Assert.assertEquals("visits.txt", e.nextElement().getName());
		Assert.assertFalse(e.hasMoreElements());
		z.close();
		// delete temporary file
		Files.delete(temp);
	}
}
