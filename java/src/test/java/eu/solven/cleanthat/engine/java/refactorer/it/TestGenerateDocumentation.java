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
package eu.solven.cleanthat.engine.java.refactorer.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.google.common.base.Strings;

import eu.solven.cleanthat.engine.java.IJdkVersionConstants;
import eu.solven.cleanthat.engine.java.refactorer.cases.AParameterizesRefactorerCases;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutator;
import eu.solven.cleanthat.engine.java.refactorer.meta.IMutatorExternalReferences;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftCompositeMutators;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.AllIncludingDraftSingleMutators;
import eu.solven.cleanthat.engine.java.refactorer.mutators.composite.CompositeMutator;
import eu.solven.cleanthat.engine.java.refactorer.test.ARefactorerCases;
import eu.solven.cleanthat.engine.java.refactorer.test.LocalClassTestHelper;

// BEWARE: This will generate a versioned file: It may lead to unexpected result. However, it will also make sure this file is often up-to-date
public class TestGenerateDocumentation {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestGenerateDocumentation.class);

	static final String EOL = System.lineSeparator();

	static String URL_PREFIX = "java/src/main/java/";
	static String URL_PREFIX_TEST = "java/src/test/java/";

	@Test
	public void doGenerateDocumentation() throws IOException {
		var sb = new StringBuilder();
		sb.append("# Mutators");

		addSingleMutators(sb, false);
		addSingleMutators(sb, true);
		addCompositeMutators(sb);

		sb.append(EOL).append("---").append(EOL).append(EOL).append("Generated by ");
		sb.append(this.getClass().getSimpleName()).append(']');
		var relativePath = this.getClass().getName().replace('.', '/');
		sb.append('(').append(URL_PREFIX_TEST).append(relativePath).append(".java").append(')').append(EOL);

		Path srcMainResources = LocalClassTestHelper.getSrcMainResourceFolder();

		var targetFile = srcMainResources.resolve("../../../MUTATORS.generated.MD").normalize();

		LOGGER.info("Writing into {}", targetFile);
		Files.writeString(targetFile, sb.toString(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
	}

	private static void addSingleMutators(StringBuilder sb, boolean isDraft) {
		CompositeMutator<IMutator> allSingles =
				new AllIncludingDraftSingleMutators(JavaVersion.parse(IJdkVersionConstants.LAST));

		sb.append(EOL).append("## Single Mutators").append(isDraft ? " (DRAFTs)" : " PRD-READY");

		for (IMutator mutator : allSingles.getUnderlyings()
				.stream()
				.filter(m -> isDraft == m.isDraft())
				.collect(Collectors.toList())) {
			addMutatorInfo(sb, mutator);
		}
	}

	private static void addCompositeMutators(StringBuilder sb) {
		CompositeMutator<CompositeMutator<?>> allComposites =
				new AllIncludingDraftCompositeMutators(JavaVersion.parse(IJdkVersionConstants.LAST));

		sb.append(EOL).append("## Composite Mutators");

		for (CompositeMutator<?> mutator : allComposites.getUnderlyings()) {
			addMutatorInfo(sb, mutator);
		}
	}

	private static void addMutatorInfo(StringBuilder sb, IMutator mutator) {
		sb.append(EOL).append(EOL).append("### ");
		appendLinkToClass(sb, mutator.getClass(), URL_PREFIX);
		sb.append(EOL);

		addRefToExternalRules(sb, mutator);

		if (mutator.isDraft()) {
			sb.append(EOL).append(EOL).append("isDraft");
		}

		sb.append(EOL).append(EOL).append("languageLevel: jdk").append(mutator.minimalJavaVersion());

		addExamples(sb, mutator);
	}

	private static void appendLinkToClass(StringBuilder sb, Class<?> clazz, String urlPrefix) {
		sb.append('[').append(clazz.getSimpleName()).append(']');
		var relativePath = clazz.getName().replace('.', '/');
		sb.append('(').append(urlPrefix).append(relativePath).append(".java").append(')');
	}

	private static void addExamples(StringBuilder sb, IMutator mutator) {
		try {
			// eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me.ArraysDotStreamCases
			String mutatorPackage = mutator.getClass().getPackageName();
			String casesPackage = mutatorPackage.substring(0, mutatorPackage.lastIndexOf('.'));
			String casesClassSimpleName = mutator.getClass().getSimpleName() + "Cases";
			String casesClassName = casesPackage + ".cases.do_not_format_me." + casesClassSimpleName;

			if (!ClassUtils.isPresent(casesClassName, null)) {
				// happens typically on composite IMutator
				LOGGER.info("There is no cases for {}", mutator.getClass().getSimpleName());
				return;
			}

			Class<?> casesClass = Class.forName(casesClassName);
			ARefactorerCases<?, ?, ?> casesInstance =
					(ARefactorerCases<?, ?, ?>) casesClass.getConstructor().newInstance();

			// JavaParser javaParser = JavaRefactorer.makeDefaultJavaParser(JavaRefactorer.JAVAPARSER_JRE_ONLY);

			Collection<Object[]> cases = AParameterizesRefactorerCases.listCases(casesInstance);

			// We print only the first test. We assume it is the most relevant one to get the rule
			cases.stream().findFirst().ifPresent(oneCaseDetails -> {
				// JavaParser javaParser = (JavaParser) oneCaseDetails[0];
				ClassOrInterfaceDeclaration oneCase = (ClassOrInterfaceDeclaration) oneCaseDetails[2];

				sb.append(EOL)
						.append(EOL)
						.append("```")
						.append(EOL)
						.append(oneCase.toString())
						.append(EOL)
						.append("```");
			});

			sb.append(EOL).append(EOL).append("See");

			appendLinkToClass(sb, casesClass, URL_PREFIX_TEST);
			;
		} catch (ReflectiveOperationException | IOException e) {
			LOGGER.warn("Issue with {}", mutator, e);
		}
	}

	private static void addRefToExternalRules(StringBuilder sb, IMutator mutator) {
		mutator.getPmdId().ifPresent(ruleId -> {
			String url = ((IMutatorExternalReferences) mutator).pmdUrl();

			sb.append(EOL).append(EOL);
			if (Strings.isNullOrEmpty(url)) {
				sb.append("PMD: ").append(ruleId);
			} else {
				sb.append("PMD: [").append(ruleId).append("](").append(url).append(')');
			}
		});
		mutator.getCheckstyleId().ifPresent(ruleId -> {
			String url = ((IMutatorExternalReferences) mutator).checkstyleUrl();

			sb.append(EOL).append(EOL);
			if (Strings.isNullOrEmpty(url)) {
				sb.append("CheckStyle: ").append(ruleId);
			} else {
				sb.append("CheckStyle: [").append(ruleId).append("](").append(url).append(')');
			}
		});
		mutator.getSonarId().ifPresent(ruleId -> {
			String url = ((IMutatorExternalReferences) mutator).sonarUrl();

			sb.append(EOL).append(EOL);
			if (Strings.isNullOrEmpty(url)) {
				sb.append("Sonar: ").append(ruleId);
			} else {
				sb.append("Sonar: [").append(ruleId).append("](").append(url).append(')');
			}
		});
		mutator.getErrorProneId().ifPresent(ruleId -> {
			String url = ((IMutatorExternalReferences) mutator).errorProneUrl();

			sb.append(EOL).append(EOL);
			if (Strings.isNullOrEmpty(url)) {
				sb.append("ErrorProne: ").append(ruleId);
			} else {
				sb.append("ErrorProne: [").append(ruleId).append("](").append(url).append(')');
			}
		});
		mutator.getJSparrowId().ifPresent(ruleId -> {
			String url = ((IMutatorExternalReferences) mutator).jSparrowUrl();

			sb.append(EOL).append(EOL);
			if (Strings.isNullOrEmpty(url)) {
				sb.append("jSparrow: ").append(ruleId);
			} else {
				sb.append("jSparrow: [").append(ruleId).append("](").append(url).append(')');
			}
		});
	}
}
