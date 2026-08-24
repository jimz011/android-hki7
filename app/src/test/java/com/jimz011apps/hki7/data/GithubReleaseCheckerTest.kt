package com.jimz011apps.hki7.data

import com.jimz011apps.hki7.data.GithubReleaseChecker.InstallSource
import com.jimz011apps.hki7.data.GithubReleaseChecker.shouldAnnounceGithubRelease
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubReleaseCheckerTest {

    @Test
    fun `a sideloaded build hears about a release as soon as it is published`() {
        assertTrue(shouldAnnounceGithubRelease(InstallSource.SIDELOAD, playHasUpdate = false))
        assertTrue(shouldAnnounceGithubRelease(InstallSource.SIDELOAD, playHasUpdate = true))
    }

    @Test
    fun `a play build stays quiet while the release is still rolling out`() {
        // The window this exists for: published on GitHub, not yet offered by Play. Announcing it
        // would send someone to a listing showing the version they already have.
        assertFalse(shouldAnnounceGithubRelease(InstallSource.PLAY, playHasUpdate = false))
    }

    @Test
    fun `a play build hears about the release once play can install it`() {
        assertTrue(shouldAnnounceGithubRelease(InstallSource.PLAY, playHasUpdate = true))
    }

    @Test
    fun `newer versions compare greater`() {
        assertTrue(GithubReleaseChecker.compareVersions("1.1.4", "1.1.3") > 0)
        assertTrue(GithubReleaseChecker.compareVersions("1.1.10", "1.1.9") > 0)
        assertTrue(GithubReleaseChecker.compareVersions("1.1.3", "1.1.3") == 0)
        assertTrue(GithubReleaseChecker.compareVersions("1.1.2", "1.1.3") < 0)
    }

    @Test
    fun `a pre-release sorts below the release it leads to`() {
        assertTrue(GithubReleaseChecker.compareVersions("1.2.0-beta.1", "1.2.0") < 0)
        assertTrue(GithubReleaseChecker.compareVersions("1.2.0", "1.2.0-beta.1") > 0)
    }
}
