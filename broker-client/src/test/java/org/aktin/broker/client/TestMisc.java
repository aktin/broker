package org.aktin.broker.client;

import javax.net.ssl.KeyManagerFactory;

import org.junit.Test;

public class TestMisc {

	@Test
	public void misc(){
		System.out.println("KeyManagerFactory.defaultAlgo: "+KeyManagerFactory.getDefaultAlgorithm());
	}

}
