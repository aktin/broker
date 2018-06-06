package org.aktin.broker.download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import javax.inject.Singleton;



@Singleton
public class DownloadManager {
	private static final Logger log = Logger.getLogger(DownloadManager.class.getName());

	private long expirationMillis;
	private Hashtable<UUID, Download> store;

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
	public DownloadImpl createLocalDownload(Path file, String mediaType) {
		DownloadImpl download = new DownloadImpl(mediaType, System.currentTimeMillis()+expirationMillis);
		download.setPath(file, false);
		download.id = UUID.randomUUID();
		store.put(download.id, download);
		log.info("Download added with UUID "+download.id);
		return download;
	}

	/**
	 * Create a temporary file for download. Once the download
	 * expires, the file will be deleted.
	 * @return download
	 * @throws IOException IO error
	 */
	public DownloadImpl createTemporaryFile(String mediaType) throws IOException {
		DownloadImpl download = new DownloadImpl(mediaType, System.currentTimeMillis()+expirationMillis);
		download.setPath(Files.createTempFile("download",null), true);
		download.id = UUID.randomUUID();
		store.put(download.id, download);
		log.info("Download added with UUID "+download.id);
		return download;
	}

	/**
	 * Cleanup expired downloads.
	 * @throws IOException io error
	 */
	public void cleanupExpired() throws IOException{
		Iterator<Entry<UUID,Download>> i = store.entrySet().iterator();
		long now = System.currentTimeMillis();
		while( i.hasNext() ) {
			Download entry = i.next().getValue();
			if( entry.getExpireTimestamp() < now ) {
				log.info("Expired download "+entry.getId());
				i.remove();
				entry.postRemovalCleanup();
			}
		}
	}


}
