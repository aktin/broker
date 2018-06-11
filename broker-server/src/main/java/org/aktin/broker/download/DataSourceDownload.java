package org.aktin.broker.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;

import org.aktin.broker.PathDataSource;
import org.aktin.broker.server.DateDataSource;

/**
 * Wraps a {@link DataSource} for download. This class also
 * recognizes {@link DateDataSource} and provides 
 * @author Raphael
 *
 */
public class DataSourceDownload extends AbstractDownload {
	private static final Logger log = Logger.getLogger(DataSourceDownload.class.getName());

	private DataSource ds;
	private boolean deletePath;
	private String name;

	public DataSourceDownload(DataSource ds) {
		this.ds = ds;
	}
	public DataSourceDownload(PathDataSource ds, boolean deletePath) {
		this(ds);
		this.deletePath = deletePath;
	}

	/**
	 * Override the name used for the download. If unset or {@code null}
	 * then the {@link DataSource#getName()} method of the underlying
	 * {@link DataSource} is used.
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public Instant getLastModified() {
		if( ds instanceof DateDataSource ) {
			return ((DateDataSource)ds).getLastModified();
		}else {
			return null;
		}
	}

	@Override
	public String getContentType() {
		return ds.getContentType();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return ds.getInputStream();
	}

	/**
	 * return the name set via {@link #setName(String)}. If this is
	 * undefined, returns the {@link DataSource#getName()}.
	 */
	@Override
	public String getName() {
		if( this.name != null ) {
			return this.name;
		}else {
			return ds.getName();
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return ds.getOutputStream();
	}

	@Override
	void postRemovalCleanup() {
		if( deletePath ) {
			if( !(ds instanceof PathDataSource) ) {
				throw new IllegalStateException("deletePath=true but unexpected data source class "+ds.getClass().getName());
			}
			Path path = ((PathDataSource)ds).getPath();
			try {
				Files.delete(path);
				log.info("Download "+id+" file "+path+" deleted");
			} catch (IOException e) {
				log.log(Level.WARNING, "Doanload "+id+" failed to delete file "+path, e);
			}
		}else {
			log.info("Download "+id+" removed. Data source remaining: "+ds);
		}
	}

	@Override
	public Long getContentLength() {
		if( ds instanceof DateDataSource ) {
			return ((DateDataSource)ds).getContentLength();
		}else {
			return null;
		}
	}

}
