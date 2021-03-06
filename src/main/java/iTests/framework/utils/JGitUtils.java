package iTests.framework.utils;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

public class JGitUtils {

    public static void clone(String localGitRepoPath, String repositoryUrl, String branchName) throws IOException {
        clone(localGitRepoPath,repositoryUrl,branchName, null, null);
    }

    public static void clone(String localGitRepoPath, String repositoryUrl, String branchName, String username, String password) throws IOException {
        if (!new File(localGitRepoPath).exists()) {
            try {
                LogUtils.log("Cloning cloudify-recipes repo to " + localGitRepoPath);
                if (username!= null && password !=null){
                    // credentials
                    CredentialsProvider cp = new UsernamePasswordCredentialsProvider(username, password);
                    Git.cloneRepository()
                            .setCredentialsProvider(cp)
                            .setURI(repositoryUrl)
                            .setDirectory(new File(localGitRepoPath))
                            .call();
                }
                else {
                    Git.cloneRepository()
                            .setURI(repositoryUrl)
                            .setDirectory(new File(localGitRepoPath))
                            .call();
                }
                if (!branchName.equalsIgnoreCase("master")) {
                    LogUtils.log("Branch under test is : " + branchName);
                    Git git = Git.open(new File(localGitRepoPath));
                    LogUtils.log("Current branch is : " + git.getRepository().getFullBranch());
                    CheckoutCommand checkout = git.checkout();
                    LogUtils.log("Checking out to " + branchName);
                    checkout.setCreateBranch(true)
                            .setName(branchName)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                            setStartPoint("origin/" + branchName)
                            .call();
                    LogUtils.log("Current branch is : " + git.getRepository().getFullBranch());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to clone " + repositoryUrl, e);
            }
        }
    }

    public static void commit(Repository localRepo, String msg) {
        Git git = null;
        try {
            git = new Git(localRepo);
            git.commit()
                    .setMessage(msg)
                    .call();
        } catch (Exception e) {
            throw new RuntimeException("Failed to commit with messgae " + msg + " to local repository " + localRepo, e);
        } finally {
            git = null;
        }
    }

    public static void push(Repository localRepo) {
        Git git = null;
        try {
            git = new Git(localRepo);
            git.push().call();
        } catch (Exception e) {
            throw new RuntimeException("Failed to push to local repository " + localRepo, e);
        } finally {
            git = null;
        }
    }

    public static void pull(Repository localRepo) {
        Git git = null;
        try {
            git = new Git(localRepo);
            git.pull().call();
        } catch (Exception e) {
            throw new RuntimeException("Failed to pull to local repository " + localRepo, e);
        } finally {
            git = null;
        }
    }

}
