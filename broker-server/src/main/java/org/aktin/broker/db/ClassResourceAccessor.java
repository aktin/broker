package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import liquibase.resource.ResourceAccessor;

public class ClassResourceAccessor implements ResourceAccessor {
	private Class<?> clazz;
	
	public ClassResourceAccessor(Class<?> clazz) {
		this.clazz = clazz;
	}
	@Override
	public Set<InputStream> getResourcesAsStream(String path) throws IOException {
		// TODO implement single element set
		return Stream.of(clazz.getResourceAsStream(path)).collect(Collectors.toSet());
	}

	@Override
	public Set<String> list(String relativeTo, String path, boolean includeFiles, boolean includeDirectories,
			boolean recursive) throws IOException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public ClassLoader toClassLoader() {
		return clazz.getClassLoader();
	}

}
