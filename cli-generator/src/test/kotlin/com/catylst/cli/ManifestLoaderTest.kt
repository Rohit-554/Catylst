package com.catylst.cli

import com.catylst.cli.model.loadManifest
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ManifestLoaderTest {

    @Test
    fun `test manifest loads successfully`() {
        val manifest = loadManifest()
        assertNotNull(manifest)
        assertTrue(manifest.features.isNotEmpty(), "Features should not be empty")
        assertTrue(manifest.features.any { it.id == "ai" }, "Should have AI feature")
        assertTrue(manifest.features.any { it.id == "ktor" }, "Should have Ktor feature")
        assertTrue(manifest.features.any { it.id == "room" }, "Should have Room feature")
    }

    @Test
    fun `test all feature requires resolve`() {
        val manifest = loadManifest()
        val featureIds = manifest.features.map { it.id }.toSet()

        for (feature in manifest.features) {
            for (req in feature.requires) {
                assertTrue(req in featureIds, "Feature '${feature.id}' requires '$req' which does not exist")
            }
        }
    }

    @Test
    fun `test sample code perFeature keys match feature ids`() {
        val manifest = loadManifest()
        val featureIds = manifest.features.map { it.id }.toSet()
        val sampleKeys = manifest.sampleCode.perFeature.keys

        for (key in sampleKeys) {
            assertTrue(key in featureIds, "Sample code key '$key' does not match any feature ID")
        }
    }
}
