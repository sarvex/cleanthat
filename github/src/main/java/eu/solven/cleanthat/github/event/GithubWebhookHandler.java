package eu.solven.cleanthat.github.event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.kohsuke.github.GHAppCreateTokenBuilder;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import cormoran.pepper.collection.PepperMapHelper;
import eu.solven.cleanthat.github.IStringFormatter;

/**
 * Default implementation for IGithubWebhookHandler
 * 
 * @author Benoit Lacelle
 *
 */
public class GithubWebhookHandler implements IGithubWebhookHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(GithubWebhookHandler.class);

	final GitHub github;
	final ObjectMapper objectMapper;

	public GithubWebhookHandler(GitHub github, ObjectMapper objectMapper) {
		this.github = github;
		this.objectMapper = objectMapper;
	}

	@Override
	public GitHub getGithub() {
		return github;
	}

	@Override
	public GitHub makeInstallationGithub(long installationId) {
		GitHub githubAuthAsInst;
		try {
			GHAppInstallation installationById = github.getApp().getInstallationById(installationId);

			LOGGER.info("Permissions: {}", installationById.getPermissions());
			LOGGER.info("RepositorySelection: {}", installationById.getRepositorySelection());

			// https://github.com/hub4j/github-api/issues/570
			GHAppCreateTokenBuilder installationGithub = installationById.createToken(Map.of(
					// Required to open new pul-requests
					"pull_requests",
					GHPermissionType.WRITE,
					// Required to access a repository without having to list all available repositories
					"metadata",
					GHPermissionType.READ,
					// Required to read files, and commit new versions
					"contents",
					GHPermissionType.WRITE));

			// https://github.com/hub4j/github-api/issues/570
			githubAuthAsInst = makeInstallationGithub(installationGithub.create().getToken());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return githubAuthAsInst;
	}

	protected GitHub makeInstallationGithub(String token) throws IOException {
		return new GitHubBuilder().withAppInstallationToken(token).build();
	}

	@Override
	public Map<String, ?> processWebhookBody(Map<String, ?> input, IStringFormatter formatter) {
		// https://developer.github.com/webhooks/event-payloads/

		long installationId = PepperMapHelper.getRequiredNumber(input, "installation", "id").longValue();
		LOGGER.info("Received a webhook for installationId={}", installationId);
		GitHub githubAuthAsInst = makeInstallationGithub(installationId);

		Optional<String> ref = PepperMapHelper.getOptionalString(input, "ref");

		GHRepository repoId;
		try {
			repoId = githubAuthAsInst.getRepositoryById(
					Long.toString(PepperMapHelper.getRequiredNumber(input, "repository", "id").longValue()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Optional<GHPullRequest> optPr;
		if (ref.isPresent()) {
			// https://developer.github.com/webhooks/event-payloads/#push
			// if (!"created".equals(action)) {
			// LOGGER.info("We are not interested in action={}", action);
			// return Map.of("action", "discarded");
			// } else {
			LOGGER.info("We are notified of a new commit: {}", ref.get());
			// }

			optPr = Optional.empty();
		} else {
			String action = PepperMapHelper.getRequiredString(input, "action");
			Map<String, ?> pullRequest = PepperMapHelper.getAs(input, "pull_request");

			if (pullRequest == null) {
				LOGGER.info("We are not interested in action={} as no pull_request", action);
				optPr = Optional.empty();
			} else {
				// https://developer.github.com/webhooks/event-payloads/#pull_request
				if (!"opened".equals(action)) {
					LOGGER.info("We are not interested in action={}", action);
					return Map.of("action", "discarded");
				} else {
					String url = PepperMapHelper.getRequiredString(input, "pull_request", "url");
					int prId = PepperMapHelper.getRequiredNumber(input, "pull_request", "id").intValue();
					LOGGER.info("We are notified of a new PR: {}", url);

					try {
						optPr = Optional.of(repoId.getPullRequest(prId));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
		}

		if (optPr.isPresent()) {
			// try {
			new GithubPullRequestCleaner(objectMapper, formatter)
					.formatPR(Optional.empty(), new AtomicInteger(), optPr.get());
			// repoId.createIssue("test").create();
			// } catch (IOException e) {
			// throw new UncheckedIOException(e);
			// }
		}

		// https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#http-based-git-access-by-an-installation
		// git clone https://x-access-token:<token>@github.com/owner/repo.git

		return Map.of();
	}

}
