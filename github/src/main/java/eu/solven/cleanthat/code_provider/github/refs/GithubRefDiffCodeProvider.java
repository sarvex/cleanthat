package eu.solven.cleanthat.code_provider.github.refs;

import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;

import eu.solven.cleanthat.codeprovider.ICodeProvider;
import eu.solven.cleanthat.codeprovider.ICodeProviderWriter;
import eu.solven.cleanthat.codeprovider.IListOnlyModifiedFiles;

/**
 * An {@link ICodeProvider} for Github pull-requests
 *
 * @author Benoit Lacelle
 */
public class GithubRefDiffCodeProvider extends AGithubHeadRefDiffCodeProvider
		implements IListOnlyModifiedFiles, ICodeProviderWriter {
	final GHRef base;

	public GithubRefDiffCodeProvider(String token, GHRepository baseRepository, GHRef base, GHRef head) {
		super(token, baseRepository, head);
		this.base = base;
	}

	/**
	 * base refName, starting with 'refs/'
	 */
	@Override
	protected String getBaseId() {
		return base.getRef();
	}

}
