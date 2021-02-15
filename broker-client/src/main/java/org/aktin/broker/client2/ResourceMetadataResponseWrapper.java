package org.aktin.broker.client2;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

import org.aktin.broker.client.ResponseWithMetadata;
import org.aktin.broker.client.Utils;

public class ResourceMetadataResponseWrapper implements ResponseWithMetadata {
	private String name;
	private HttpResponse<InputStream> resp;

	public ResourceMetadataResponseWrapper(String name, HttpResponse<InputStream> resp) {
		this.name = name;
		this.resp = resp;
	}
	@Override
	public long getLastModified() {
		Optional<String> str = resp.headers().firstValue(AbstractBrokerClient.LAST_MODIFIED_HEADER);
		if( str.isPresent() ) {
			return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(str.get())).getEpochSecond();
		}else {
			return 0;
		}
	}

	@Override
	public byte[] getMD5() {
		Optional<String> base64 = resp.headers().firstValue(AbstractBrokerClient.CONTENTMD5_HEADER);
		if( base64.isPresent() ) {
			return Base64.getUrlDecoder().decode(base64.get());
		}else {
			return null;
		}
	}

	@Override
	public String getMD5String() {
		byte[] md5 = getMD5();
		if( md5 == null ) {
			return null;
		}
		return Utils.toHexString(md5);
	}

	@Override
	public String getContentType() {
		return resp.headers().firstValue(AbstractBrokerClient.CONTENT_TYPE_HEADER).orElse(null);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return resp.body();
	}

}
