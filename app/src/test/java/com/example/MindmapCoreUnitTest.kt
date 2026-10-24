package com.example

import com.example.data.export.MindmapExporter
import com.example.data.model.MindNode
import com.example.data.model.Mindmap
import com.example.data.vector.VectorDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MindmapCoreUnitTest {

    @Test
    fun testVectorDatabaseSemanticSearch() {
        val vectorDb = VectorDatabase.getInstance()
        val node1 = MindNode(
            id = "test_n1",
            mindmapId = "map1",
            title = "Photosynthesis Light Reactions",
            notesMarkdown = "Chlorophyll pigments absorb light energy and produce ATP and NADPH in chloroplasts."
        )
        val node2 = MindNode(
            id = "test_n2",
            mindmapId = "map1",
            title = "Binary Search Tree",
            notesMarkdown = "Left subtree keys are less than root key, right subtree keys are greater."
        )

        vectorDb.indexNode(node1, "Biology")
        vectorDb.indexNode(node2, "Computer Science")

        val results = vectorDb.search("chloroplast sunlight energy", topK = 5)
        assertTrue("Expected search results for sunlight energy", results.isNotEmpty())
        assertEquals("test_n1", results[0].record.nodeId)
        assertTrue("Similarity score should be positive", results[0].similarityScore > 0f)
    }

    @Test
    fun testMindmapExporterJsonRoundTrip() {
        val map = Mindmap(id = "map_export_1", title = "Quantum Mechanics", subject = "PHYS-301")
        val nodes = listOf(
            MindNode(id = "n_root", mindmapId = map.id, title = "Wave-Particle Duality", notesMarkdown = "De Broglie wavelength"),
            MindNode(id = "n_child", mindmapId = map.id, parentId = "n_root", title = "Schrodinger Equation", notesMarkdown = "H psi = E psi")
        )

        val json = MindmapExporter.exportToJson(map, nodes)
        assertTrue(json.contains("Wave-Particle Duality"))
        assertTrue(json.contains("Schrodinger Equation"))

        val (importedMap, importedNodes) = MindmapExporter.importFromJson(json, "user_123")
        assertEquals("map_export_1", importedMap.id)
        assertEquals("Quantum Mechanics", importedMap.title)
        assertEquals(2, importedNodes.size)
        assertEquals("Wave-Particle Duality", importedNodes[0].title)
    }

    @Test
    fun testMindmapExporterMarkdownAndSvg() {
        val map = Mindmap(id = "map_svg", title = "Algorithms", subject = "CS-101")
        val nodes = listOf(
            MindNode(id = "root", mindmapId = map.id, title = "Divide and Conquer"),
            MindNode(id = "c1", mindmapId = map.id, parentId = "root", title = "Merge Sort")
        )

        val md = MindmapExporter.exportToMarkdown(map, nodes)
        assertTrue(md.contains("# Algorithms"))
        assertTrue(md.contains("Divide and Conquer"))
        assertTrue(md.contains("Merge Sort"))

        val svg = MindmapExporter.exportToSvg(map, nodes)
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("Divide and Conquer"))
        assertTrue(svg.contains("</svg>"))
    }
}
