// Copyright 2011 The MOE Authors All Rights Reserved.

package com.google.devtools.moe.client.dvcs.hg;

import static org.easymock.EasyMock.expect;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.devtools.moe.client.CommandRunner;
import com.google.devtools.moe.client.FileSystem;
import com.google.devtools.moe.client.FileSystem.Lifetime;
import com.google.devtools.moe.client.Lifetimes;
import com.google.devtools.moe.client.project.RepositoryConfig;
import com.google.devtools.moe.client.testing.ExtendedTestModule;

import dagger.ObjectGraph;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.io.File;

/**
 * Unit tests for HgClonedRepository: verify that Hg cloning works as expected.
 *
 */
public class HgClonedRepositoryTest extends TestCase {
  private final IMocksControl control = EasyMock.createControl();
  private final FileSystem mockFS = control.createMock(FileSystem.class);
  private final CommandRunner cmd = control.createMock(CommandRunner.class);
  private final RepositoryConfig repositoryConfig = control.createMock(RepositoryConfig.class);

  private final String repositoryName = "mockrepo";
  private final String repositoryURL = "http://foo/hg";

  public void testCloneLocally() throws Exception {
    ObjectGraph graph = ObjectGraph.create(new ExtendedTestModule(mockFS, cmd));
    graph.injectStatics();

    expect(repositoryConfig.getUrl()).andReturn(repositoryURL).anyTimes();
    expect(repositoryConfig.getBranch()).andReturn(Optional.of("mybranch")).anyTimes();
    String localCloneTempDir = "/tmp/hg_clone_mockrepo_12345";

    // The Lifetimes of clones in these tests are arbitrary since we're not really creating any
    // temp dirs and we're not testing clean-up.
    expect(mockFS.getTemporaryDirectory(
        EasyMock.eq("hg_clone_" + repositoryName + "_"), EasyMock.<Lifetime>anyObject()))
        .andReturn(new File(localCloneTempDir));

    expect(
        cmd.runCommand(
            "hg",
            ImmutableList.<String>of(
                "clone",
                repositoryURL,
                localCloneTempDir,
                "--rev=" + "mybranch"),
            "" /*workingDirectory*/))
        .andReturn("hg clone ok (mock output)");
    expect(cmd.runCommand("hg", ImmutableList.of("branch"), localCloneTempDir))
        .andReturn("mybranch");

    // Run test
    control.replay();

    HgClonedRepository repo = new HgClonedRepository(repositoryName, repositoryConfig);
    repo.cloneLocallyAtHead(Lifetimes.persistent());

    assertEquals(repositoryName, repo.getRepositoryName());
    assertEquals(repositoryURL, repo.getConfig().getUrl());
    assertEquals(localCloneTempDir, repo.getLocalTempDir().getAbsolutePath());

    try {
      repo.cloneLocallyAtHead(Lifetimes.persistent());
      fail("Re-cloning repo succeeded unexpectedly.");
    } catch (IllegalStateException expected) {}

    control.verify();
  }
}
