package org.aktin.broker.client;

import javax.net.ssl.KeyManagerFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TestMisc {

	@Test
	public void misc(){
		// output algorithm
		String defaultAlgo = KeyManagerFactory.getDefaultAlgorithm();
		Assertions.assertNotNull(defaultAlgo);
		System.out.println("KeyManagerFactory.defaultAlgo: "+defaultAlgo);
	}

}
