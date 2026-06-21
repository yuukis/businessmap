package com.github.yuukis.businessmap.util

import org.junit.Assert.assertEquals
import org.junit.Test

class StringJUtilsTest {

    @Test
    fun `hankakuKatakanaToZenkakuKatakana converts a single character`() {
        assertEquals('ア', StringJUtils.hankakuKatakanaToZenkakuKatakana('ｱ'))
    }

    @Test
    fun `hankakuKatakanaToZenkakuKatakana leaves non-katakana characters untouched`() {
        assertEquals('A', StringJUtils.hankakuKatakanaToZenkakuKatakana('A'))
    }

    @Test
    fun `hankakuKatakanaToZenkakuKatakana converts a plain string`() {
        assertEquals("アイウエオ", StringJUtils.hankakuKatakanaToZenkakuKatakana("ｱｲｳｴｵ"))
    }

    @Test
    fun `hankakuKatakanaToZenkakuKatakana merges voiced sound marks`() {
        assertEquals("ガギグゲゴ", StringJUtils.hankakuKatakanaToZenkakuKatakana("ｶﾞｷﾞｸﾞｹﾞｺﾞ"))
    }

    @Test
    fun `hankakuKatakanaToZenkakuKatakana merges semi-voiced sound marks`() {
        assertEquals("パピプペポ", StringJUtils.hankakuKatakanaToZenkakuKatakana("ﾊﾟﾋﾟﾌﾟﾍﾟﾎﾟ"))
    }

    @Test
    fun `hankakuKatakanaToZenkakuKatakana handles a single trailing character after merging`() {
        assertEquals("ガア", StringJUtils.hankakuKatakanaToZenkakuKatakana("ｶﾞｱ"))
    }

    @Test
    fun `zenkakuHiraganaToZenkakuKatakana converts hiragana only`() {
        assertEquals("ヤマダタロウABC123", StringJUtils.zenkakuHiraganaToZenkakuKatakana("やまだたろうABC123"))
    }

    @Test
    fun `convertToKatakana converts hiragana to katakana`() {
        assertEquals("ヤマダタロウ", StringJUtils.convertToKatakana("やまだたろう"))
    }

    @Test
    fun `convertToKatakana handles mixed hankaku katakana and hiragana input`() {
        assertEquals("ガギグゲゴア", StringJUtils.convertToKatakana("ｶﾞｷﾞｸﾞｹﾞｺﾞあ"))
    }
}
