package com.middleeastcontainer.data.export

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a real .xlsx with no library.
 *
 * An .xlsx is a ZIP of XML parts, so the whole format can be emitted directly.
 * The alternative on Android is Apache POI, which adds tens of megabytes to the
 * APK and is slow to initialise on a phone — a heavy price for a sheet with six
 * columns.
 *
 * Strings are written inline rather than through a shared-strings table. Slightly
 * larger on disk, but it removes an entire part and a second pass over the data.
 */
object XlsxWriter {

    /** A cell is either text or a number; numbers must not be quoted. */
    sealed interface Cell {
        data class Text(val value: String) : Cell
        data class Number(val value: Long) : Cell
    }

    fun text(value: String?): Cell = Cell.Text(value.orEmpty())
    fun number(value: Long): Cell = Cell.Number(value)

    /**
     * Writes [rows] as a single sheet.
     *
     * The stream is not closed here — the caller owns it, and on Android it is
     * usually a MediaStore descriptor with its own lifecycle.
     */
    fun write(out: OutputStream, sheetName: String, rows: List<List<Cell>>) {
        val zip = ZipOutputStream(out)
        zip.put("[Content_Types].xml", CONTENT_TYPES)
        zip.put("_rels/.rels", ROOT_RELS)
        zip.put("xl/workbook.xml", workbook(sheetName))
        zip.put("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
        zip.put("xl/worksheets/sheet1.xml", sheet(rows))
        zip.finish()
        zip.flush()
    }

    private fun ZipOutputStream.put(name: String, body: String) {
        putNextEntry(ZipEntry(name))
        write(body.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun sheet(rows: List<List<Cell>>): String = buildString {
        append(HEAD)
        append("<worksheet xmlns=\"$NS_MAIN\"><sheetData>")
        rows.forEachIndexed { r, row ->
            append("<row r=\"${r + 1}\">")
            row.forEachIndexed { c, cell ->
                val ref = columnName(c) + (r + 1)
                when (cell) {
                    is Cell.Number -> append("<c r=\"$ref\"><v>${cell.value}</v></c>")
                    is Cell.Text -> append(
                        "<c r=\"$ref\" t=\"inlineStr\"><is><t>${escape(cell.value)}</t></is></c>"
                    )
                }
            }
            append("</row>")
        }
        append("</sheetData></worksheet>")
    }

    /** 0 -> A, 25 -> Z, 26 -> AA. */
    private fun columnName(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + i % 26))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    /**
     * Escapes XML, and strips control characters.
     *
     * A stray control byte from OCR would make the file unopenable, and Excel
     * reports that as corruption rather than as a bad character.
     */
    private fun escape(value: String): String = buildString {
        for (ch in value) {
            when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch == '\'' -> append("&apos;")
                ch.code < 0x20 && ch != '\t' && ch != '\n' -> Unit
                else -> append(ch)
            }
        }
    }

    private fun workbook(sheetName: String) =
        HEAD + "<workbook xmlns=\"$NS_MAIN\" xmlns:r=\"$NS_REL\"><sheets>" +
            "<sheet name=\"${escape(sheetName.take(31))}\" sheetId=\"1\" r:id=\"rId1\"/>" +
            "</sheets></workbook>"

    private const val HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships"

    private val CONTENT_TYPES = HEAD +
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
        "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
        "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
        "</Types>"

    private val ROOT_RELS = HEAD +
        "<Relationships xmlns=\"$NS_PKG_REL\">" +
        "<Relationship Id=\"rId1\" Type=\"$NS_REL/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>"

    private val WORKBOOK_RELS = HEAD +
        "<Relationships xmlns=\"$NS_PKG_REL\">" +
        "<Relationship Id=\"rId1\" Type=\"$NS_REL/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
        "</Relationships>"
}
