package com.sap.sse.landscape.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.landscape.Release;
import com.sap.sse.util.ThreadPoolUtil;

public class TestGithubReleaseRepository {
    private static final String DOCKER_25 = "docker-25";
    private static final String MAIN = "main";
    private static GithubReleasesRepository repository;
    
    @BeforeAll
    public static void setUp() {
        repository = new GithubReleasesRepository("SAP", "sailing-analytics", MAIN);
    }
    
    @Test
    public void testAtLeastOneRelease() {
        assertFalse(Util.isEmpty(repository));
    }
    
    @Test
    public void testAtLeastOneMainRelease() {
        final Release latestMasterRelease = repository.getLatestDefaultRelease();
        assertNotNull(latestMasterRelease);
        assertEquals(MAIN, latestMasterRelease.getBaseName());
    }

    @Test
    public void testAtLeastOneDocker25Release() {
        final Release latestDocker25Release = repository.getLatestRelease(DOCKER_25);
        assertNotNull(latestDocker25Release);
        assertEquals(DOCKER_25, latestDocker25Release.getBaseName());
    }
    
    @Test
    public void testLinkToNextPageFromFirst() {
        assertEquals("https://api.github.com/repositories/790295432/releases?page=2",
                repository.getNextPageURL("<https://api.github.com/repositories/790295432/releases?page=2>; rel=\"next\", <https://api.github.com/repositories/790295432/releases?page=27>; rel=\"last\""));
    }

    @Test
    public void testLinkToNextPageFromMiddleWith100PerPage() {
        assertEquals("https://api.github.com/repositories/790295432/releases?per_page=100&page=5",
                repository.getNextPageURL("<https://api.github.com/repositories/790295432/releases?per_page=100&page=3>; rel=\"prev\", <https://api.github.com/repositories/790295432/releases?per_page=100&page=5>; rel=\"next\", <https://api.github.com/repositories/790295432/releases?per_page=100&page=8>; rel=\"last\", <https://api.github.com/repositories/790295432/releases?per_page=100&page=1>; rel=\"first\""));
    }
    
    @Test
    public void testLinkToNextPageFromLaste() {
        assertNull(
                repository.getNextPageURL("<https://api.github.com/repositories/790295432/releases?per_page=100&page=7>; rel=\"prev\", <https://api.github.com/repositories/790295432/releases?per_page=100&page=1>; rel=\"first\""));
    }
    
    @Disabled("Goes against a harsh GitHub rate limit of 60 requests per hour, so enable only for one-time manual tests")
    @Test
    public void testOldDocker17ReleaseExists() {
        assertFalse(Util.isEmpty(Util.filter(repository, release->release.getName().equals("docker-17-202404262046"))));
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException, ExecutionException {
        final ScheduledExecutorService threadPool = ThreadPoolUtil.INSTANCE.createForegroundTaskThreadPoolExecutor(10, getClass().getName()+":testConcurrentAccess()");
        final Map<String, Future<Release>> futures = new HashMap<>();
        final String[] prefixes = new String[] { "main", "docker-25", "docker-24", "docker-21", "docker-17" };
        for (final String prefix : prefixes) {
            futures.put(prefix, threadPool.submit(()->repository.getLatestRelease(prefix)));
        }
        for (final String prefix : prefixes) {
            assertNotNull(futures.get(prefix).get());
            assertEquals(prefix, futures.get(prefix).get().getBaseName());
        }
        threadPool.shutdown();
    }
}
