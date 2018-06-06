package org.aktin.broker.download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.inject.Singleton;

import org.aktin.broker.PathDataSource;



@Singleton
public class DownloadManager {
	private static final Logger log = Logger.getLogger(DownloadManager.class.getName());

	private long expirationMillis;
	private Hashtable<UUID, AbstractDownload> store;

	public DownloadManager() {
		store = new Hashtable<>();
		expirationMillis = 1000*60*10; // 10 minutes
	}

	public Download get(UUID id) throws IOException {
		cleanupExpired();
		return store.get(id);
	}

	/**
	 * Create download to a given local path. The path
	 * will not be deleted once the download expires.
	 * @param file path to download
	 * @return Download
	 */
	public Download createDataSourceDownload(DataSource ds) {
		DataSourceDownload download = new DataSourceDownload(ds);
		addDownload(download);
		return download;
	}

	private void addDownload(AbstractDownload download) {
		download.expiration = System.currentTimeMillis()+expirationMillis;
		download.id = UUID.randomUUID();

		store.put(download.id, download);
		log.info("Download added with UUID "+download.id);
	}
	/**
	 * Create a temporary file for download. Once the download
	 * expires, the file will be deleted.
	 * @return download
	 * @throws IOException IO error
	 */
	public Download createTemporaryFile(String mediaType) throws IOException {
		Path temp = Files.createTempFile("download",null);
		PathDataSource ds = new PathDataSource(temp, mediaType, Instant.now());
		DataSourceDownload download = new DataSourceDownload(ds, true);
		addDownload(download);
		return download;
	}

	/**
	 * Cleanup expired downloads.
	 * @throws IOException io error
	 */
	public void cleanupExpired() throws IOException{
		Iterator<Entry<UUID,AbstractDownload>> i = store.entrySet().iterator();
		long now = System.currentTimeMillis();
		while( i.hasNext() ) {
			AbstractDownload entry = i.next().getValue();
			if( entry.getExpireTimestamp() < now ) {
				log.info("Expired download "+entry.getId());
				i.remove();
				entry.postRemovalCleanup();
			}
		}
	}


}
