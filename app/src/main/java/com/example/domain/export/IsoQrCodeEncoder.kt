package com.example.domain.export

/**
 * Codificador QR Code real compatível com o padrão ISO/IEC 18004 (Modo Byte, Nível de Correção L,
 * Polinómio Reed-Solomon em Corpo de Galois GF(256) com polinómio primitivo 0x11D).
 *
 * Permite que qualquer leitor óptico real — incluindo o scanner nativo do aplicativo Google Wallet,
 * a câmara do Android/iOS e leitores de portão de embarque IATA — decodifique o payload BCBP do bilhete.
 */
object IsoQrCodeEncoder {

    private data class VersionSpec(
        val version: Int,
        val totalCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocksGroup1: Int,
        val dataCodewordsGroup1: Int,
        val numBlocksGroup2: Int,
        val dataCodewordsGroup2: Int,
        val alignmentCenters: IntArray
    ) {
        val size: Int get() = 17 + version * 4
        val totalDataCodewords: Int
            get() = numBlocksGroup1 * dataCodewordsGroup1 + numBlocksGroup2 * dataCodewordsGroup2
    }

    // Especificações oficiais ISO/IEC 18004 para Nível L (Versões 1 a 8 - suporta até 152 bytes)
    private val VERSION_SPECS_LEVEL_L = listOf(
        VersionSpec(1, 26, 7, 1, 19, 0, 0, intArrayOf()),
        VersionSpec(2, 44, 10, 1, 34, 0, 0, intArrayOf(6, 18)),
        VersionSpec(3, 70, 15, 1, 55, 0, 0, intArrayOf(6, 22)),
        VersionSpec(4, 100, 20, 1, 80, 0, 0, intArrayOf(6, 26)),
        VersionSpec(5, 134, 26, 1, 108, 0, 0, intArrayOf(6, 30)),
        VersionSpec(6, 172, 18, 2, 68, 0, 0, intArrayOf(6, 34)),
        VersionSpec(7, 196, 20, 4, 39, 0, 0, intArrayOf(6, 22, 38)),
        VersionSpec(8, 242, 24, 2, 97, 0, 0, intArrayOf(6, 24, 42))
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun reedSolomonGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val root = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
            }
            poly = next
        }
        return poly
    }

    private fun reedSolomonRemainder(data: IntArray, degree: Int): IntArray {
        val gen = reedSolomonGeneratorPoly(degree)
        val result = IntArray(degree)
        for (byteVal in data) {
            val factor = byteVal xor result[0]
            System.arraycopy(result, 1, result, 0, degree - 1)
            result[degree - 1] = 0
            if (factor != 0) {
                for (j in 0 until degree) {
                    result[j] = result[j] xor gfMul(gen[j + 1], factor)
                }
            }
        }
        return result
    }

    /**
     * Gera a matriz booleana [size][size] do QR Code ISO/IEC 18004 para o texto informado.
     */
    fun encodeToMatrix(payload: String): Array<BooleanArray> {
        val rawBytes = payload.trim()
            .ifBlank { "M1PASSENGER/AGUIAFLY EAGF999 LISCDGTP 1024 319C002A0001 100" }
            .encodeToByteArray()

        // Garante que caiba na versão 8 Level L (máximo 192 bytes; truncamos de forma segura em 180 bytes)
        val dataBytes = if (rawBytes.size > 180) rawBytes.copyOfRange(0, 180) else rawBytes

        val spec = VERSION_SPECS_LEVEL_L.firstOrNull { v ->
            // 4 bits modo (0100) + 8 bits tamanho (versões 1..9) + N*8 bits <= totalDataCodewords*8
            (4 + 8 + dataBytes.size * 8) <= v.totalDataCodewords * 8
        } ?: VERSION_SPECS_LEVEL_L.last()

        val dataCodewords = buildDataCodewords(dataBytes, spec.totalDataCodewords)
        val finalCodewords = interleaveWithErrorCorrection(dataCodewords, spec)

        val size = spec.size
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        // 1. Finder patterns + separadores
        placeFinderPattern(modules, isFunction, 0, 0)
        placeFinderPattern(modules, isFunction, size - 7, 0)
        placeFinderPattern(modules, isFunction, 0, size - 7)

        // 2. Timing patterns
        for (i in 8 until size - 8) {
            val dark = (i % 2 == 0)
            modules[6][i] = dark
            isFunction[6][i] = true
            modules[i][6] = dark
            isFunction[i][6] = true
        }

        // 3. Alignment patterns
        val centers = spec.alignmentCenters
        for (r in centers) {
            for (c in centers) {
                val overlapsFinder = (r <= 8 && c <= 8) ||
                    (r <= 8 && c >= size - 9) ||
                    (r >= size - 9 && c <= 8)
                if (!overlapsFinder) {
                    placeAlignmentPattern(modules, isFunction, r, c)
                }
            }
        }

        // 4. Dark module + reservar áreas de formato e versão
        modules[size - 8][8] = true
        isFunction[size - 8][8] = true

        for (i in 0..8) {
            if (i < size) {
                isFunction[8][i] = true
                isFunction[i][8] = true
            }
        }
        for (i in 0..7) {
            isFunction[8][size - 1 - i] = true
            isFunction[size - 1 - i][8] = true
        }
        if (spec.version >= 7) {
            for (i in 0 until 6) {
                for (j in 0 until 3) {
                    isFunction[size - 11 + j][i] = true
                    isFunction[i][size - 11 + j] = true
                }
            }
        }

        // 5. Preencher bits de dados em ziguezague
        placeDataBits(modules, isFunction, finalCodewords)

        // 6. Aplicar máscara 0 ((row + col) % 2 == 0) apenas nos módulos de dados
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!isFunction[r][c] && (r + c) % 2 == 0) {
                    modules[r][c] = !modules[r][c]
                }
            }
        }

        // 7. Escrever bits de formato (Nível L = 01, Máscara 0 = 000 -> Format Info = 0x77C4)
        placeFormatBits(modules, size, 0x77C4)

        // 8. Se versão >= 7, escrever bits de versão
        if (spec.version >= 7) {
            placeVersionBits(modules, size, spec.version)
        }

        return modules
    }

    private fun buildDataCodewords(bytes: ByteArray, capacityCodewords: Int): IntArray {
        val bits = ArrayList<Int>(capacityCodewords * 8 + 16)
        fun appendBits(value: Int, length: Int) {
            for (i in length - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        // Mode indicator: Byte mode = 0100 (4 bits)
        appendBits(0b0100, 4)
        // Character count indicator (8 bits para versões 1..9 em modo Byte)
        appendBits(bytes.size, 8)
        for (b in bytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        val maxBits = capacityCodewords * 8
        val terminatorLen = minOf(4, maxBits - bits.size)
        appendBits(0, terminatorLen)

        while (bits.size % 8 != 0) {
            bits.add(0)
        }

        val result = IntArray(capacityCodewords)
        var idx = 0
        var bitIdx = 0
        while (bitIdx + 7 < bits.size && idx < capacityCodewords) {
            var byteVal = 0
            for (b in 0 until 8) {
                byteVal = (byteVal shl 1) or bits[bitIdx + b]
            }
            result[idx++] = byteVal
            bitIdx += 8
        }

        val padBytes = intArrayOf(0xEC, 0x11)
        var padToggle = 0
        while (idx < capacityCodewords) {
            result[idx++] = padBytes[padToggle % 2]
            padToggle++
        }
        return result
    }

    private fun interleaveWithErrorCorrection(dataCodewords: IntArray, spec: VersionSpec): IntArray {
        val dataBlocks = ArrayList<IntArray>()
        val ecBlocks = ArrayList<IntArray>()
        var offset = 0

        repeat(spec.numBlocksGroup1) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataCodewordsGroup1)
            offset += spec.dataCodewordsGroup1
            dataBlocks.add(block)
            ecBlocks.add(reedSolomonRemainder(block, spec.ecCodewordsPerBlock))
        }
        repeat(spec.numBlocksGroup2) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataCodewordsGroup2)
            offset += spec.dataCodewordsGroup2
            dataBlocks.add(block)
            ecBlocks.add(reedSolomonRemainder(block, spec.ecCodewordsPerBlock))
        }

        val result = IntArray(spec.totalCodewords)
        var outIdx = 0
        val maxDataLen = maxOf(spec.dataCodewordsGroup1, spec.dataCodewordsGroup2)
        for (i in 0 until maxDataLen) {
            for (block in dataBlocks) {
                if (i < block.size) {
                    result[outIdx++] = block[i]
                }
            }
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (ec in ecBlocks) {
                if (i < ec.size && outIdx < result.size) {
                    result[outIdx++] = ec[i]
                }
            }
        }
        return result
    }

    private fun placeFinderPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        top: Int,
        left: Int
    ) {
        val size = modules.size
        for (dr in -1..7) {
            for (dc in -1..7) {
                val r = top + dr
                val c = left + dc
                if (r in 0 until size && c in 0 until size) {
                    val isBorder = dr in 0..6 && (dc == 0 || dc == 6) ||
                        dc in 0..6 && (dr == 0 || dr == 6)
                    val isCenter = dr in 2..4 && dc in 2..4
                    modules[r][c] = isBorder || isCenter
                    isFunction[r][c] = true
                }
            }
        }
    }

    private fun placeAlignmentPattern(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        centerRow: Int,
        centerCol: Int
    ) {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val r = centerRow + dr
                val c = centerCol + dc
                val dist = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
                modules[r][c] = (dist != 1)
                isFunction[r][c] = true
            }
        }
    }

    private fun placeDataBits(
        modules: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        codewords: IntArray
    ) {
        val size = modules.size
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var right = size - 1
        var upward = true

        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                val r = if (upward) size - 1 - vert else vert
                for (j in 0..1) {
                    val c = right - j
                    if (!isFunction[r][c]) {
                        val dark = if (bitIndex < totalBits) {
                            val cw = codewords[bitIndex ushr 3]
                            ((cw ushr (7 - (bitIndex and 7))) and 1) != 0
                        } else {
                            false
                        }
                        modules[r][c] = dark
                        bitIndex++
                    }
                }
            }
            upward = !upward
            right -= 2
        }
    }

    private fun placeFormatBits(modules: Array<BooleanArray>, size: Int, formatBits: Int) {
        for (i in 0..5) modules[8][i] = ((formatBits ushr i) and 1) != 0
        modules[8][7] = ((formatBits ushr 6) and 1) != 0
        modules[8][8] = ((formatBits ushr 7) and 1) != 0
        modules[7][8] = ((formatBits ushr 8) and 1) != 0
        for (i in 9..14) modules[14 - i][8] = ((formatBits ushr i) and 1) != 0

        for (i in 0..7) modules[size - 1 - i][8] = ((formatBits ushr i) and 1) != 0
        for (i in 8..14) modules[8][size - 15 + i] = ((formatBits ushr i) and 1) != 0
    }

    private fun placeVersionBits(modules: Array<BooleanArray>, size: Int, version: Int) {
        var rem = version
        repeat(12) {
            rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25)
        }
        val bits = (version shl 12) or rem
        for (i in 0 until 18) {
            val dark = ((bits ushr i) and 1) != 0
            val a = size - 11 + (i % 3)
            val b = i / 3
            modules[a][b] = dark
            modules[b][a] = dark
        }
    }
}
