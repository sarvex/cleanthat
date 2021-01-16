package github;

import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import eu.solven.cleanthat.formatter.CodeProviderFormatter;

// https://facelessuser.github.io/wcmatch/glob/
public class TestCodeProviderFormatter {
	@Test
	public void testMatchFile_root_absolute() {
		List<PathMatcher> matchers = CodeProviderFormatter.prepareMatcher(CodeProviderFormatter.DEFAULT_INCLUDES_JAVA);

		Optional<PathMatcher> matching = CodeProviderFormatter.findMatching(matchers, "/SomeClass.java");

		Assert.assertTrue(matching.isPresent());
	}

	// In this case, we suppose the issue would be to return a relative path
	@Test
	public void testMatchFile_root_relative() {
		List<PathMatcher> matchers = CodeProviderFormatter.prepareMatcher(CodeProviderFormatter.DEFAULT_INCLUDES_JAVA);

		Optional<PathMatcher> matching = CodeProviderFormatter.findMatching(matchers, "SomeClass.java");

		Assert.assertTrue(matching.isEmpty());
	}

	@Test
	public void testMatchFile_subFolder() {
		List<PathMatcher> matchers = CodeProviderFormatter.prepareMatcher(CodeProviderFormatter.DEFAULT_INCLUDES_JAVA);

		Optional<PathMatcher> matching = CodeProviderFormatter.findMatching(matchers, "src/main/java/SomeClass.java");

		Assert.assertTrue(matching.isPresent());
	}
}
