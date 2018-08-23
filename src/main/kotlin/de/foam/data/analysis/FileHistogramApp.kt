package de.foam.data.analysis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.text.Font
import javafx.util.StringConverter
import tornadofx.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * @author jobusam
 *
 * This implementation creates several charts (bar chart, line chart, pie chart) and uses
 * as data input the file size of all files from a given directory (define data directories in DataCollection class (see TODO)!
 *
 * The charts shall display the amount of files depending on the file size.
 * Additionally the extracted data from the input directories will be stored as JSON File
 * to reuse it for the next application class. Delete this JSON Data Cache and the data
 * will be extracted from the given input directories!
 *
 * For java fx support with openjdk under fedora:
 * install sudo dnf install openjfx openjfx-devel java-1.8.0-openjdk-openjfx java-1.8.0-openjdk-openjfx-devel
 * Caution: Afterwards you have to reimport the sdk classpath under
 * "File -> Project Structure -> Platform Settings -> SDKs within the Class Path Tab!
 * Otherwise the IntelliJ Idea doesn't recognise the newly add jfxrt.jar!
 */

//File path for temporary save create data in JSON format!
const val JSON_FILE_CACHE = "imageDataSample.json"

fun main(args: Array<String>) {
    launch<MasterApp>(args)
}

class DataCollection {
    companion object {
        val images: List<Image>

        init {
            //If data was previously created it will be loaded from serialized json file.
            //Otherwise create the new data! This implementation shall improve performance when executing multiple times
            val jsonDataCacheFile = Paths.get(JSON_FILE_CACHE)
            if (jsonDataCacheFile.toFile().exists()) {
                images = deserializeFromJSONFile(jsonDataCacheFile)
            } else {
                images = createData()
                serializeToJSON(jsonDataCacheFile)
            }
        }

        private fun createData(): List<Image> =
                listOf<Image>(
                        //TODO Define Images as Data Input
                        Image("Test Image", getFileSize(Paths.get("./")))
                )

        //val images = DataCollection().deserializeFromJSONFile(Paths.get("/home/johannes/Desktop/test/test.json"))
        private fun serializeToJSON(output: Path) {
            val mapper = ObjectMapper().registerModule(KotlinModule())
            //pretty file output
            mapper.enable(SerializationFeature.INDENT_OUTPUT)
            mapper.writeValue(output.toFile(), images)
        }

        private fun deserializeFromJSONFile(jsonInput: Path): List<Image> {
            val mapper = ObjectMapper().registerModule(KotlinModule())
            return mapper.readValue<List<Image>>(jsonInput.toFile())
        }
    }
}

data class Stat(val amount: Double, val size: Double)

data class Image(val imageName: String, val data: Map<Int, Stat>) {
    val amountOfFiles = data.values.map { it.amount }.sum()
    val imageSize = data.values.map { it.size }.sum()
}

class MasterApp : App(MasterView::class)

class MasterView : View() {

    private val lineChart = linechart("Häufigkeit nach Dateigröße",
            createCategoryAxis(), createNumberAxis()) {
        DataCollection.images.forEach { image ->
            series(createDisplayName(image)) {
                image.data.forEach { data(convertToCategory(it.key), it.value.amount) }
            }
        }
    }

    private val cumulatedLineChartAmount = linechart("Kumulierte Häufigkeit nach Dateigröße",
            createCategoryAxis(), createNumberAxis()) {
        DataCollection.images.forEach { image ->
            series(createDisplayName(image)) {
                cumulateMapContent(image.data.mapValues { it.value.amount }).forEach { data(convertToCategory(it.key), it.value) }
            }
        }
    }

    private val relativeCumulatedLineChartAmount = linechart("Relativierte kumulierte Häufigkeit nach Dateigröße",
            createCategoryAxis(), createNumberAxis("Anzahl der Dateien / Gesamtanzahl (in %)")) {
        DataCollection.images.forEach { image ->
            series(createDisplayName(image)) {
                relativateMapContent(cumulateMapContent(image.data.mapValues { it.value.amount }), image.amountOfFiles)
                        .forEach { data(convertToCategory(it.key), it.value) }
            }
        }
    }

    private val cumulatedLineChartSize = linechart("Kumulierte Kategoriegröße nach Dateigröße",
            createCategoryAxis(), createNumberAxisFileSizeinByte("Kategoriegröße in GB")) {
        DataCollection.images.forEach { image ->
            series(createDisplayName(image)) {
                cumulateMapContent(image.data.mapValues { it.value.size }).forEach { data(convertToCategory(it.key), it.value) }
            }
        }
    }

    private val relativeCumulatedLineChartSize = linechart("Relativierte kumulierte Kategoriegröße nach Dateigröße",
            createCategoryAxis(), createNumberAxis("Dateigröße der Kategorie / Gesamtgröße (in %)")) {
        DataCollection.images.forEach { image ->
            series(createDisplayName(image)) {
                relativateMapContent(cumulateMapContent(image.data.mapValues { it.value.size }), image.imageSize).forEach { data(convertToCategory(it.key), it.value) }
            }
        }
    }

    private val barChart = barchart("Häufigkeit nach Dateigröße",
            createCategoryAxis(), createNumberAxis()) {
        DataCollection.images.forEach { image ->
            series(createDisplayName(image)) {
                image.data.forEach { data(convertToCategory(it.key), it.value.amount) }
            }
        }
    }

    private val pieChart = piechart("Häufigkeit nach Dateigröße von Abbild ${DataCollection.images.getOrNull(0)?.imageName}") {
        DataCollection.images.getOrNull(0)?.data?.forEach { data(convertToCategory(it.key), it.value.amount) }
    }


    override val root = tabpane {
        title = "File Size Analysis"
        tab("Line Chart - Amount", lineChart)
        tab("Cumulative Line Chart - Amount", cumulatedLineChartAmount)
        tab("Relative Cumulative Line Chart - Amount", relativeCumulatedLineChartAmount)
        tab("Bar Chart - Amount", barChart)
        tab("Pie Chart -Amount", pieChart)
        tab("Cumulative Line Chart - Size", cumulatedLineChartSize)
        tab("Relative Cumulative Line Chart - Size", relativeCumulatedLineChartSize)

    }

    private fun createCategoryAxis(): CategoryAxis {
        val categoryAxis = CategoryAxis()
        categoryAxis.label = "Dateikategorie aufgeteilt nach Dateigröße (in Bytes)"
        categoryAxis.tickLabelFontProperty().set(Font.font(13.0))
        return categoryAxis
    }

    private fun createNumberAxis(label: String = "Anzahl der Dateien"): NumberAxis {
        val numberAxis = NumberAxis()
        numberAxis.label = label
        numberAxis.tickLabelFormatterProperty().set(object : StringConverter<Number>() {
            override fun fromString(p0: String?): Number = p0?.toInt() ?: 0
            override fun toString(n: Number?): String = NumberFormat.getInstance(Locale.GERMANY).format(n)
        })
        numberAxis.tickLabelFontProperty().set(Font.font(13.0))
        return numberAxis
    }

    //TODO following configuration contains special hard coded data range and units! Update if necessary.
    private fun createNumberAxisFileSizeinByte(label: String ): NumberAxis {
        // CAUTION: This axis will be hardcoded with tick unit of 50 GB!
        val numberAxis = NumberAxis(label,0.0,250.0 * 1024 * 1024 * 1024,25.0 * 1024 * 1024 * 1024)
        numberAxis.tickLabelFormatterProperty().set(object : StringConverter<Number>() {
            override fun fromString(p0: String?): Number = p0?.toInt() ?: 0
            override fun toString(n: Number?): String {
                //FIXME This is quick and dirty hack to display the fileSize directly in GB
                val displayValue = (n?.toDouble() ?: 1.0) / (1024 * 1024 * 1024)
                return "${NumberFormat.getInstance(Locale.GERMANY).format(displayValue)} GB"
            }
        })
        numberAxis.tickLabelFontProperty().set(Font.font(13.0))
        return numberAxis
    }


    /**
     * create Image Name and add file size in GB to the name!
     */
    private fun createDisplayName(image: Image): String {
        val sizeInGb = image.imageSize / (1024 * 1024 * 1024)
        return "${image.imageName} (~ ${sizeInGb.roundToInt()} GB)"
    }
}

fun convertToCategory(value: Int): String {
    val map = hashMapOf(0 to "10 B", 1 to "100 B",
            2 to "1 KB", 3 to "10 KB", 4 to "100 KB",
            5 to "1 MB", 6 to "10 MB", 7 to "100 MB",
            8 to "1 GB", 9 to "10 GB", 10 to "100 GB")
    return map[value] ?: "???"
}

fun getFileSize(directory: Path): Map<Int, Stat> {

    val output = HashMap<Int, Stat>()

    Files.walk(directory)
            //.parallel() //Keep in mind this can cause other problems.
            // see also https://dzone.com/articles/think-twice-using-java-8
            .filter { it.toFile().isFile }
            .map { Files.size(it) }
            .map { Stat(it.toDouble(), it.toDouble()) }
            .map { Stat(Math.max(1.0, it.amount), it.size) }
            .map { Stat(Math.log10(it.amount), it.size) }
            // following map is import to trim double values to integer
            .map { Stat(it.amount.toInt().toDouble(), it.size) }
            //.peek{System.out.println(it)}
            .forEach {
                val key = it.amount.toInt()
                val amount = (output[key]?.amount ?: 0.0) + 1.0
                val size = (output[key]?.size ?: 0.0) + it.size
                output[key] = Stat(amount, size)
            }
    return output
}

fun cumulateMapContent(input: Map<Int, Double>): Map<Int, Double> {
    val output = HashMap<Int, Double>()
    input.forEach {
        var sum = 0.0
        for (i in 0..it.key) {
            sum += input[i] ?: 0.0
        }
        output[it.key] = sum
    }
    return output
}

fun relativateMapContent(input: Map<Int, Double>, amountOfFiles: Double): Map<Int, Double> {
    val output = HashMap<Int, Double>()
    input.forEach {
        output[it.key] = it.value / amountOfFiles * 100
    }
    return output
}

