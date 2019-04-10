package org.aktin.broker.query.sql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Function;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.aktin.broker.query.QueryHandlerFactory;
import org.w3c.dom.Element;

// TODO add qualifier annotation

/**
 * SQL Handler factory.
 * @author R.W.Majeed
 *
 */
public class SQLHandlerFactory implements QueryHandlerFactory{
	private DataSource ds;
	private Charset exportCharset;
	private DateTimeFormatter formatter;
	

	public SQLHandlerFactory() {
		this.exportCharset = StandardCharsets.UTF_8;
		this.formatter = DateTimeFormatter.ISO_INSTANT;
	}
	public SQLHandlerFactory(DataSource database) {
		this();
		this.ds = database;
	}


	/**
	 * Set the data source used for executing SQL queries
	 * @param database data source
	 */
	public void setDataSource(DataSource database){
		this.ds = database;
	}
	public Charset getExportCharset(){
		return exportCharset;
	}

	/**
	 * Set the formatter used to format timestamps for the SQL execution. Some SQL
	 * dialects require specific date time formats. Defaults to
	 * {@code DateTimeFormatter.ISO_INSTANT} if this method is never called.
	 * @param formatter formatter
	 */
	public void setDateTimeFormatter(DateTimeFormatter formatter) {
		this.formatter = formatter;
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
		return formatter.format(timestamp);
	}

}
