package com.example.data.vector

import com.example.data.model.MindNode
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

data class VectorRecord(
    val nodeId: String,
    val mindmapId: String,
    val title: String,
    val content: String,
    val subject: String,
    val vector: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VectorRecord
        return nodeId == other.nodeId && mindmapId == other.mindmapId
    }

    override fun hashCode(): Int {
        var result = nodeId.hashCode()
        result = 31 * result + mindmapId.hashCode()
        return result
    }
}

data class VectorSearchResult(
    val record: VectorRecord,
    val similarityScore: Float,
    val snippet: String
)

class VectorDatabase private constructor() {
    companion object {
        const val VECTOR_DIM = 64
        private val STOP_WORDS = setOf(
            "the", "a", "an", "and", "or", "in", "on", "at", "to", "for", "of", "with",
            "is", "are", "was", "were", "this", "that", "these", "those", "it", "as", "by"
        )

        @Volatile
        private var INSTANCE: VectorDatabase? = null

        fun getInstance(): VectorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VectorDatabase().also { INSTANCE = it }
            }
        }
    }

    private val records = mutableMapOf<String, VectorRecord>()

    /**
     * Compute a normalized dense vector embedding using hashing-based TF-IDF representation.
     */
    fun embedText(text: String): FloatArray {
        val vec = FloatArray(VECTOR_DIM)
        val clean = text.lowercase().replace(Regex("[^a-z0-9\\s]"), " ")
        val tokens = clean.split("\\s+".toRegex()).filter { it.length > 2 && it !in STOP_WORDS }
        if (tokens.isEmpty()) return vec

        // Frequency table
        val tf = mutableMapOf<String, Int>()
        for (token in tokens) {
            tf[token] = (tf[token] ?: 0) + 1
            // Sub-word character trigrams for semantic morphology
            if (token.length >= 4) {
                for (i in 0..token.length - 3) {
                    val tri = token.substring(i, i + 3)
                    tf[tri] = (tf[tri] ?: 0) + 1
                }
            }
        }

        for ((token, count) in tf) {
            val hash = abs(token.hashCode())
            val index = hash % VECTOR_DIM
            val sign = if ((hash shr 8) % 2 == 0) 1.0f else -1.0f
            val weight = (1.0f + ln(count.toDouble()).toFloat())
            vec[index] += sign * weight
        }

        // L2 normalize vector
        var norm = 0.0f
        for (v in vec) norm += v * v
        if (norm > 0f) {
            val length = sqrt(norm)
            for (i in vec.indices) {
                vec[i] /= length
            }
        }
        return vec
    }

    /**
     * Index a mind node into the vector database
     */
    fun indexNode(node: MindNode, subject: String = "General") {
        val combinedText = "${node.title} ${node.notesMarkdown} ${node.tags.joinToString(" ")}"
        val vector = embedText(combinedText)
        val record = VectorRecord(
            nodeId = node.id,
            mindmapId = node.mindmapId,
            title = node.title,
            content = node.notesMarkdown,
            subject = subject,
            vector = vector
        )
        records[node.id] = record
    }

    fun indexNodes(nodes: List<MindNode>, subject: String = "General") {
        for (node in nodes) {
            indexNode(node, subject)
        }
    }

    fun removeNode(nodeId: String) {
        records.remove(nodeId)
    }

    /**
     * Semantic vector search across all indexed student knowledge
     */
    fun search(query: String, topK: Int = 10, minSimilarity: Float = 0.15f): List<VectorSearchResult> {
        val queryVec = embedText(query)
        if (queryVec.all { it == 0f }) return emptyList()

        val results = mutableListOf<VectorSearchResult>()
        for (record in records.values) {
            val sim = cosineSimilarity(queryVec, record.vector)
            if (sim >= minSimilarity) {
                val snippet = if (record.content.length > 120) {
                    record.content.take(120) + "..."
                } else if (record.content.isNotBlank()) {
                    record.content
                } else {
                    "Title: ${record.title}"
                }
                results.add(VectorSearchResult(record, sim, snippet))
            }
        }
        return results.sortedByDescending { it.similarityScore }.take(topK)
    }

    fun size(): Int = records.size

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dot = 0.0f
        for (i in 0 until VECTOR_DIM) {
            dot += v1[i] * v2[i]
        }
        return dot.coerceIn(0.0f, 1.0f)
    }
}
