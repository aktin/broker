package org.aktin.broker.query.sql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.aktin.broker.query.QueryHandlerFactory;
import org.w3c.dom.Element;

// TODO add qualifier annotation

public class SQLHandlerFactory implements QueryHandlerFactory{
	private DataSource ds;
	private Charset exportCharset;

	public SQLHandlerFactory() {
		this.exportCharset = StandardCharsets.UTF_8;
	}
	public SQLHandlerFactory(DataSource database) {
		this();
		this.ds = database;
	}
	

	public void setDataSource(DataSource database){
		this.ds = database;
	}
	public Charset getExportCharset(){
		return exportCharset;
	}

	Connection openConnection() throws SQLException{
		Objects.requireNonNull(ds, "datasource not set");
		return ds.getConnection();
	}
	@Override
	public String getElementName() {
		return SQLQuery.XML_ELEMENT;
	}

	@Override
	public String getNamespace() {
		return SQLQuery.XML_NAMESPACE;
	}

	@Override
	public SQLHandler parse(Element element, Function<String,String> propertyLookup) {
		SQLQuery q;
		try {
			JAXBContext c = JAXBContext.newInstance(SQLQuery.class);
			q = (SQLQuery)c.createUnmarshaller().unmarshal(element);
		} catch (JAXBException e) {
			throw new IllegalArgumentException("Unable to parse query XML", e);
		}
		return wrap(q, propertyLookup);
	}

	public SQLHandler wrap(SQLQuery query, Function<String,String> propertyLookup){
		return new SQLHandler(this, query, propertyLookup);
	}
	@Override
	public String formatTimestamp(Instant timestamp) {
		return timestamp.toString();
	}

}
