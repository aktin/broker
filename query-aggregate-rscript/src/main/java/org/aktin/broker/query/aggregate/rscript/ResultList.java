package org.aktin.broker.query.aggregate.rscript;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

@XmlAccessorType(XmlAccessType.NONE)

public class ResultList {
	@XmlAttribute
	String file;

	@XmlTransient
	List<Result> fileEntries;

	@XmlTransient
	List<Result> getFileEntries(Function<String, Path> resolve) throws IOException{
		if( fileEntries == null ) {
			// lazy load entries
			Properties p = new Properties();
			try( InputStream in = Files.newInputStream(resolve.apply(file)) ){
				p.load(in);
			}
			final List<Result> entries = new ArrayList<>(p.size());
			p.forEach( (k,v) -> entries.add(new Result(k.toString(), v.toString())));
			this.fileEntries = entries;
		}
		return fileEntries;
	}
}
