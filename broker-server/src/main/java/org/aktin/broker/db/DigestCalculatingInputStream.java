package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestCalculatingInputStream extends InputStream {
	private MessageDigest md[];
	private InputStream source;
	private byte[][] digest;

	public DigestCalculatingInputStream(InputStream source, String[] algorithms) throws NoSuchAlgorithmException{
		this.source = source;
		this.md = new MessageDigest[algorithms.length];
		for( int i=0; i<algorithms.length; i++ ){
			md[i] = MessageDigest.getInstance(algorithms[i]);
		}
	}

	public byte[][] getDigests(){
		return digest;
	}

	private void updateDigests(byte[] data, int off, int len){
		for( int i=0; i<md.length; i++ ){
			md[i].update(data, off, len);
		}
	}
	private void updateDigests(byte data){
		for( int i=0; i<md.length; i++ ){
			md[i].update(data);
		}
	}
	private void calculateDigests(){
		digest = new byte[md.length][];
		for( int i=0; i<md.length; i++ ){
			digest[i] = md[i].digest();
		}
	}

	@Override
	public int available() throws IOException {
		return source.available();
	}
	@Override
	public void close() throws IOException {
		if( digest == null ){
			calculateDigests();
		}
		source.close();
	}
	@Override
	public boolean markSupported() {
		return false;
	}
	@Override
	public int read(byte[] dest, int off, int len) throws IOException {
		int cb = source.read(dest, off, len);
		if( cb > 0 ){
			updateDigests(dest, off, cb);
		}else if( cb == -1 && digest == null ){
			calculateDigests();
		}
		return cb;
	}
	@Override
	public int read(byte[] dest) throws IOException {
		int cb = source.read(dest);
		if( cb > 0 ){
			updateDigests(dest, 0, cb);
		}else if( cb == -1 && digest == null ){
			calculateDigests();
		}
		return cb;
	}
	@Override
	public long skip(long arg0) throws IOException {
		// TODO read to digest and don't return
		throw new UnsupportedOperationException();
	}
	@Override
	public int read() throws IOException {
		int b = source.read();
		if( b != -1 ){
			updateDigests((byte)b);
		}else if( digest == null ){
			// calculate digest
			calculateDigests();
		}
		return b;
	}

}
