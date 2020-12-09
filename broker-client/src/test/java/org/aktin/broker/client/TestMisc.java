package org.aktin.broker.client;

import javax.net.ssl.KeyManagerFactory;

import org.junit.Assert;
import org.junit.Test;

public class TestMisc {

	@Test
	public void misc(){
		// output algorithm
		String defaultAlgo = KeyManagerFactory.getDefaultAlgorithm();
		Assert.assertNotNull(defaultAlgo);
		System.out.println("KeyManagerFactory.defaultAlgo: "+defaultAlgo);
	}

}
