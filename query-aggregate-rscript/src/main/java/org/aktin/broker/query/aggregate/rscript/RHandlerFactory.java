package org.aktin.broker.query.aggregate.rscript;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Function;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.aktin.broker.query.QueryHandlerFactory;
import org.w3c.dom.Element;

// TODO add qualifier annotation

public class RHandlerFactory implements QueryHandlerFactory{
	private Charset exportCharset;
	private Path rExecPath;

	public RHandlerFactory(Path rExecPath) {
		this.exportCharset = StandardCharsets.UTF_8;
		this.rExecPath = rExecPath;
	}

	public Charset getExportCharset(){
		return exportCharset;
	}

	public Path getRExecutablePath() {
		return rExecPath;
	}

	@Override
	public String getElementName() {
		return RSource.XML_ELEMENT;
	}

	@Override
	public String getNamespace() {
		return RSource.XML_NAMESPACE;
	}

	@Override
	public RHandler parse(Element element, Function<String,String> propertyLookup) {
		RSource q;
		try {
			JAXBContext c = JAXBContext.newInstance(RSource.class);
			q = (RSource)c.createUnmarshaller().unmarshal(element);
		} catch (JAXBException e) {
			throw new IllegalArgumentException("Unable to parse query XML", e);
		}
		return wrap(q, propertyLookup);
	}

	public RHandler wrap(RSource query, Function<String,String> propertyLookup){
		return new RHandler(this, query, propertyLookup);
	}
	@Override
	public String formatTimestamp(Instant timestamp) {
		return timestamp.toString();
	}
}
