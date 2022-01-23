package eu.solven.cleanthat.code_provider.github.refs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import eu.solven.cleanthat.code_provider.github.code_provider.AGithubCodeProvider;
import eu.solven.cleanthat.codeprovider.DummyCodeProviderFile;
import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderFile;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public abstract class AGithubDiffCodeProvider extends AGithubCodeProvider implements IListOnlyModifiedFiles {
	private static final Logger LOGGER = LoggerFactory.getLogger(AGithubDiffCodeProvider.class);

	private static final int LIMIT_COMMIT_IN_COMPARE = 250;

	final String token;
	final GHRepository baseRepository;

	final Supplier<GHCompare> diffSupplier;

	public AGithubDiffCodeProvider(String token, GHRepository baseRepository) {
		this.token = token;

		this.baseRepository = baseRepository;

		// https://stackoverflow.com/questions/26925312/github-api-how-to-compare-2-commits
		this.diffSupplier = Suppliers.memoize(() -> {
			try {
				return baseRepository.getCompare(getBaseId(), getHeadId());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	protected abstract String getBaseId();

	protected abstract String getHeadId();

	@Override
	public void listFiles(Consumer<ICodeProviderFile> consumer) throws IOException {
		GHCompare diff = diffSupplier.get();

		if (diff.getTotalCommits() >= LIMIT_COMMIT_IN_COMPARE) {
			// https://stackoverflow.com/questions/26925312/github-api-how-to-compare-2-commits
			// https://developer.github.com/v3/repos/commits/#list-commits-on-a-repository
			LOGGER.warn("We are considering a diff of more than 250 Commits ({})", diff.getTotalCommits());
		}

		Stream.of(diff.getFiles()).forEach(prFile -> {
			// Github does not prefix with '/'
			// TODO What is the rational of requiring leading '/'?
			String fileName = "/" + prFile.getFileName();
			if ("removed".equals(prFile.getStatus())) {
				LOGGER.debug("Skip a removed file: {}", fileName);
			} else {
				consumer.accept(new DummyCodeProviderFile(fileName, prFile));
			}
		});
	}

	@Override
	public String getHtmlUrl() {
		return diffSupplier.get().getHtmlUrl().toExternalForm();
	}

	@Override
	public String getTitle() {
		return getHtmlUrl();
	}

	@Override
	public String deprecatedLoadContent(Object rawFile) throws IOException {
		GHPullRequestFileDetail file = (GHPullRequestFileDetail) rawFile;
		return loadContent(baseRepository, file.getFilename(), null);
	}

	@Override
	public String deprecatedGetFilePath(Object file) {
		return ((GHPullRequestFileDetail) file).getFilename();
	}

	@Override
	public Optional<String> loadContentForPath(String path) throws IOException {
		try {
			return Optional.of(loadContent(baseRepository, path, getHeadId()));
		} catch (GHFileNotFoundException e) {
			LOGGER.trace("We miss: {}", path, e);
			LOGGER.debug("We miss: {}", path);
			return Optional.empty();
		}
	}

	@Override
	public String getRepoUri() {
		return baseRepository.getGitTransportUrl();
	}

}
