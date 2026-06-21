package com.github.yuukis.businessmap.util

object StringJUtils {

    private val HANKAKU_KATAKANA = charArrayOf(
        '｡', '｢', '｣', '､', '･',
        'ｦ', 'ｧ', 'ｨ', 'ｩ', 'ｪ', 'ｫ', 'ｬ', 'ｭ', 'ｮ', 'ｯ', 'ｰ', 'ｱ', 'ｲ',
        'ｳ', 'ｴ', 'ｵ', 'ｶ', 'ｷ', 'ｸ', 'ｹ', 'ｺ', 'ｻ', 'ｼ', 'ｽ', 'ｾ', 'ｿ',
        'ﾀ', 'ﾁ', 'ﾂ', 'ﾃ', 'ﾄ', 'ﾅ', 'ﾆ', 'ﾇ', 'ﾈ', 'ﾉ', 'ﾊ', 'ﾋ', 'ﾌ',
        'ﾍ', 'ﾎ', 'ﾏ', 'ﾐ', 'ﾑ', 'ﾒ', 'ﾓ', 'ﾔ', 'ﾕ', 'ﾖ', 'ﾗ', 'ﾘ', 'ﾙ',
        'ﾚ', 'ﾛ', 'ﾜ', 'ﾝ', 'ﾞ', 'ﾟ'
    )

    private val ZENKAKU_KATAKANA = charArrayOf(
        '。', '「', '」', '、', '・',
        'ヲ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ッ', 'ー', 'ア', 'イ',
        'ウ', 'エ', 'オ', 'カ', 'キ', 'ク', 'ケ', 'コ', 'サ', 'シ', 'ス', 'セ', 'ソ',
        'タ', 'チ', 'ツ', 'テ', 'ト', 'ナ', 'ニ', 'ヌ', 'ネ', 'ノ', 'ハ', 'ヒ', 'フ',
        'ヘ', 'ホ', 'マ', 'ミ', 'ム', 'メ', 'モ', 'ヤ', 'ユ', 'ヨ', 'ラ', 'リ', 'ル',
        'レ', 'ロ', 'ワ', 'ン', '゛', '゜'
    )

    private val HANKAKU_KATAKANA_FIRST_CHAR = HANKAKU_KATAKANA[0]

    private val HANKAKU_KATAKANA_LAST_CHAR = HANKAKU_KATAKANA[HANKAKU_KATAKANA.size - 1]

    /**
     * 半角カタカナから全角カタカナへ変換します。
     *
     * @param c 変換前の文字
     * @return 変換後の文字
     */
    @JvmStatic
    fun hankakuKatakanaToZenkakuKatakana(c: Char): Char {
        return if (c in HANKAKU_KATAKANA_FIRST_CHAR..HANKAKU_KATAKANA_LAST_CHAR) {
            ZENKAKU_KATAKANA[c - HANKAKU_KATAKANA_FIRST_CHAR]
        } else {
            c
        }
    }

    @JvmStatic
    fun mergeChar(c1: Char, c2: Char): Char {
        if (c2 == 'ﾞ') {
            if ("ｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾊﾋﾌﾍﾎ".indexOf(c1) >= 0) {
                return when (c1) {
                    'ｶ' -> 'ガ'
                    'ｷ' -> 'ギ'
                    'ｸ' -> 'グ'
                    'ｹ' -> 'ゲ'
                    'ｺ' -> 'ゴ'
                    'ｻ' -> 'ザ'
                    'ｼ' -> 'ジ'
                    'ｽ' -> 'ズ'
                    'ｾ' -> 'ゼ'
                    'ｿ' -> 'ゾ'
                    'ﾀ' -> 'ダ'
                    'ﾁ' -> 'ヂ'
                    'ﾂ' -> 'ヅ'
                    'ﾃ' -> 'デ'
                    'ﾄ' -> 'ド'
                    'ﾊ' -> 'バ'
                    'ﾋ' -> 'ビ'
                    'ﾌ' -> 'ブ'
                    'ﾍ' -> 'ベ'
                    'ﾎ' -> 'ボ'
                    else -> c1
                }
            }
        } else if (c2 == 'ﾟ') {
            if ("ﾊﾋﾌﾍﾎ".indexOf(c1) >= 0) {
                return when (c1) {
                    'ﾊ' -> 'パ'
                    'ﾋ' -> 'ピ'
                    'ﾌ' -> 'プ'
                    'ﾍ' -> 'ペ'
                    'ﾎ' -> 'ポ'
                    else -> c1
                }
            }
        }
        return c1
    }

    @JvmStatic
    fun hankakuKatakanaToZenkakuKatakana(s: String): String {
        if (s.isEmpty()) {
            return s
        } else if (s.length == 1) {
            return hankakuKatakanaToZenkakuKatakana(s[0]).toString()
        } else {
            val sb = StringBuilder(s)
            var i = 0
            while (i < sb.length - 1) {
                val originalChar1 = sb[i]
                val originalChar2 = sb[i + 1]
                val mergedChar = mergeChar(originalChar1, originalChar2)
                if (mergedChar != originalChar1) {
                    sb.setCharAt(i, mergedChar)
                    sb.deleteCharAt(i + 1)
                } else {
                    val convertedChar = hankakuKatakanaToZenkakuKatakana(originalChar1)
                    if (convertedChar != originalChar1) {
                        sb.setCharAt(i, convertedChar)
                    }
                }
                i++
            }
            if (i < sb.length) {
                val originalChar1 = sb[i]
                val convertedChar = hankakuKatakanaToZenkakuKatakana(originalChar1)
                if (convertedChar != originalChar1) {
                    sb.setCharAt(i, convertedChar)
                }
            }
            return sb.toString()
        }
    }

    @JvmStatic
    fun zenkakuHiraganaToZenkakuKatakana(s: String): String {
        val sb = StringBuilder(s)
        for (i in sb.indices) {
            val c = sb[i]
            if (c in 'ぁ'..'ん') {
                sb.setCharAt(i, (c - 'ぁ' + 'ァ'.code).toChar())
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun convertToKatakana(s: String): String {
        var result = hankakuKatakanaToZenkakuKatakana(s)
        result = zenkakuHiraganaToZenkakuKatakana(result)
        return result
    }
}
