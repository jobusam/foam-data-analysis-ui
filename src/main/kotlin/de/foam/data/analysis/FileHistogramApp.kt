package de.foam.data.analysis

import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.text.Font
import javafx.util.StringConverter
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path
import java.text.NumberFormat
import java.util.*

/**
 * @author jobusam
 *
 * This implementation creates several charts (bar chart, line chart, pie chart) and uses
 * as data input the file size of all files from a given directory (define data directories in Data class!
 *
 * The charts shall display the amount of files depending on the file size.
 *
 *
 * For java fx support with openjdk under fedora:
 * install sudo dnf install openjfx openjfx-devel java-1.8.0-openjdk-openjfx java-1.8.0-openjdk-openjfx-devel
 * Caution: Afterwards you have to reimport the sdk classpath under
 * "File -> Project Structure -> Platform Settings -> SDKs within the Class Path Tab!
 * Otherwise the IntelliJ Idea doesn't recognise the newly add jfxrt.jar!
 */

fun main(args: Array<String>) {

    launch<MasterApp>(args)

}

class Data {
    companion object {
        val images = listOf<Image>(
                //FIXME Define Images as Data Input
                // Image("Test Image ",getFileSize(Paths.get("/home/user/test")))
        )
    }
}

data class Image(val imageName: String, val map: Map<Int, Int>)

class MasterApp : App(MasterView::class)

class MasterView : View() {

    private val lineChart = linechart("Häufigkeit nach Dateigröße", CategoryAxis(), NumberAxis()) {
        Data.images.forEach { image ->
            series(image.imageName) {
                image.map.forEach { data(convertToCategory(it.key), it.value) }
            }
        }
    }

    private val lineChartcategoryAxis = CategoryAxis().let {
        it.label = "Dateigröße (in Byte)"
        it.tickLabelFontProperty().set(Font.font(13.0))
        it
    }
    private val lineChartnumberAxis = NumberAxis().let {
        it.label = "Anzahl der Dateien"
        it.tickLabelFormatterProperty().set(object : StringConverter<Number>() {
            override fun fromString(p0: String?): Number = p0?.toInt() ?: 0
            override fun toString(n: Number?): String = NumberFormat.getInstance(Locale.GERMANY).format(n)
        })
        it.tickLabelFontProperty().set(Font.font(13.0))
        it
    }

    private val cumulatedLineChart = linechart("Kumulierte Häufigkeit nach Dateigröße", lineChartcategoryAxis, lineChartnumberAxis) {
        Data.images.forEach { image ->
            series(image.imageName) {
                cumulateMapContent(image.map).forEach { data(convertToCategory(it.key), it.value) }
            }
        }
    }

    private val pieChart = piechart("Häufigkeit nach Dateigröße") {
        Data.images.getOrNull(0)?.map?.forEach { data(convertToCategory(it.key), it.value.toDouble()) }
    }

    private val barChartCategoryAxis = CategoryAxis().let {
        it.label = "Dateigröße (in Byte)"
        it.tickLabelFontProperty().set(Font.font(13.0))
        it
    }
    private val barChartNumberAxis = NumberAxis().let {
        it.label = "Anzahl der Dateien"
        it.tickLabelFormatterProperty().set(object : StringConverter<Number>() {
            override fun fromString(p0: String?): Number = p0?.toInt() ?: 0
            override fun toString(n: Number?): String = NumberFormat.getInstance(Locale.GERMANY).format(n)
        })
        it.tickLabelFontProperty().set(Font.font(13.0))
        it
    }


    private val barChart = barchart("Häufigkeit nach Dateigröße", barChartCategoryAxis, barChartNumberAxis) {
        Data.images.forEach { image ->
            series(image.imageName) {
                image.map.forEach { data(convertToCategory(it.key), it.value) }
            }
        }
    }

    override val root = tabpane {
        title = "File Size Analysis"
        tab("Line Chart", lineChart)
        tab("Cumulative Line Chart", cumulatedLineChart)
        tab("Bar Chart", barChart)
        tab("Pie Chart", pieChart)
    }
}

fun convertToCategory(value: Int): String {
    val map = hashMapOf(0 to "10 B", 1 to "100 B",
            2 to "1 KB", 3 to "10 KB", 4 to "100 KB",
            5 to "1 MB", 6 to "10 MB", 7 to "100 MB",
            8 to "1 GB", 9 to "10 GB", 10 to "100 GB")
    return map[value] ?: "???"
}

fun getFileSize(directory: Path): Map<Int, Int> {

    val map = HashMap<Int, Int>()

    Files.walk(directory)
            //.parallel() //Keep in mind this can cause other problems.
            // see also https://dzone.com/articles/think-twice-using-java-8
            .filter { it.toFile().isFile }
            .map { Files.size(it) }
            //.peek{System.out.println("Input = $it")}
            .map { Math.max(1, it) }
            .map { Math.log10(it.toDouble()) }
            //.peek{System.out.println(it)}
            //.map { String.format("%.2g%n",it).toDouble()}
            .map { it.toInt() }
            //.peek{System.out.println(it)}
            .forEach {
                map[it] = (map[it] ?: 0) + 1
            }
    return map
}

fun cumulateMapContent(input: Map<Int, Int>): Map<Int, Int> {
    val output = HashMap<Int, Int>()
    input.forEach {
        var sum = 0
        for (i in 0..it.key) {
            sum += input[i] ?: 0
        }
        output[it.key] = sum
    }
    return output
}

