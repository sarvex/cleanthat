/*
 * Copyright 2023 Solven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.solven.cleanthat.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.Files;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.config.pojo.CleanthatEngineProperties;
import eu.solven.cleanthat.config.pojo.CleanthatRepositoryProperties;
import eu.solven.cleanthat.engine.IEngineLintFixerFactory;

/**
 * Helps generating a default {@link CleanthatRepositoryProperties}
 * 
 * @author Benoit Lacelle
 *
 */
public class GenerateInitialConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(GenerateInitialConfig.class);

	final Collection<IEngineLintFixerFactory> factories;

	public GenerateInitialConfig(Collection<IEngineLintFixerFactory> factories) {
		this.factories = factories;
	}

	// Guess Java version: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L13
	// Detect usage of Checkstyle:
	// https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L35
	// Code formatting: https://github.com/solven-eu/spring-boot/blob/master/buildSrc/build.gradle#L17
	// https://github.com/spring-io/spring-javaformat/blob/master/src/checkstyle/checkstyle.xml
	// com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck
	public EngineInitializerResult prepareDefaultConfiguration(ICodeProvider codeProvider) throws IOException {
		Map<String, String> pathToContent = new LinkedHashMap<>();

		CleanthatRepositoryProperties properties = CleanthatRepositoryProperties.defaultRepository();

		Set<String> extentionsFound = scanFileExtentions(codeProvider);

		factories.forEach(factory -> {
			SetView<String> intersection = Sets.intersection(factory.getFileExtentions(), extentionsFound);
			if (!intersection.isEmpty()) {
				LOGGER.info("There is a file-extension match ({}) for {}", intersection, factory.getEngine());

				CleanthatEngineProperties engineProperties = factory.makeDefaultProperties();
				properties.getEngines().add(engineProperties);

				pathToContent.putAll(factory.makeCustomDefaultFiles(engineProperties));
			}
		});

		return EngineInitializerResult.builder().repoProperties(properties).pathToContents(pathToContent).build();
	}

	public Set<String> scanFileExtentions(ICodeProvider codeProvider) {
		Set<String> extentionsFound = new TreeSet<>();

		try {
			// Listing files may be slow if there is many files (e.g. download of repo as zip)
			LOGGER.info("About to list files to prepare a default configuration");
			codeProvider.listFilesForFilenames(file -> {
				String filePath = file.getPath();

				// https://stackoverflow.com/questions/924394/how-to-get-the-filename-without-the-extension-in-java
				String extention = Files.getFileExtension(filePath);

				extentionsFound.add(extention);
			});
		} catch (IOException e) {
			throw new UncheckedIOException("Issue listing all files for extentions", e);
		} catch (OutOfMemoryError e) {
			// https://github.com/hub4j/github-api/issues/1405
			// The implementation downloading the repo as zip materialized the whole zip in memory
			LOGGER.warn("Issue while processing the repository", e);
		}

		LOGGER.info("Extentions found in {}: {}", codeProvider, extentionsFound);

		return extentionsFound;
	}

}
