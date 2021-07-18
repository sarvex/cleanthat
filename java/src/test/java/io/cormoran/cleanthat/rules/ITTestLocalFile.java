package io.cormoran.cleanthat.rules;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import eu.solven.cleanthat.rules.VariableEqualsConstant;
import eu.solven.cleanthat.rules.meta.IClassTransformer;

/**
 * This is useful to investigate a misbehavior over current project file
 * 
 * @author Benoit Lacelle
 *
 */
public class ITTestLocalFile {
	private static final Logger LOGGER = LoggerFactory.getLogger(ITTestLocalFile.class);

	final String path = "./github/src/main/java/eu/solven/cleanthat/github/event/GithubWebhookHandler.java";

	@Test
	public void testCleanLocalFile() throws IOException {
		LOGGER.info("Process: {}");

		File file = new File("../" + path);

		if (!file.isFile()) {
			throw new IllegalArgumentException("Can not read: " + file.getAbsolutePath());
		}

		String pathAsString = Files.readString(file.toPath());

		CompilationUnit node = StaticJavaParser.parse(pathAsString);

		IClassTransformer rule = new VariableEqualsConstant();
		boolean changed = rule.walkNode(node);

		if (!changed) {
			throw new IllegalArgumentException(rule + " did not change: " + file.getAbsolutePath());
		}

		DiffMatchPatch dmp = new DiffMatchPatch();
		String newAsString = node.toString();

		// TODO We may need to reformat to have a nice diff
		List<Diff> diff = dmp.diffMain(pathAsString, newAsString, false);
		LOGGER.info("Diff:");
		diff.forEach(d -> LOGGER.info("{}", d));
	}
}
