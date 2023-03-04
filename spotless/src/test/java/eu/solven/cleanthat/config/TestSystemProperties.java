/*
 * Copyright 2023 Benoit Lacelle - SOLVEN
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

import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import eu.solven.cleanthat.maven_core.SystemProperties;

public class TestSystemProperties {
	@Test
	public void testSystemPropertiesIsSafe() {
		System.setProperty("TestSystemProperties.secretKey", "secretValue");

		Properties systemProperties = SystemProperties.getSystemProperties();

		Assertions.assertThat(systemProperties)
				.containsKey("java.version")
				.doesNotContainKey("TestSystemProperties.secretKey")
				.hasSizeLessThan(2);
	}
}
