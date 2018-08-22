package de.foam.data.analysis

import tornadofx.*

/**
 * For java fx support with openjdk under fedora:
 * install sudo dnf install openjfx openjfx-devel java-1.8.0-openjdk-openjfx java-1.8.0-openjdk-openjfx-devel
 * Caution: Afterwards you have to reimport the sdk classpath under
 * "File -> Project Structure -> Platform Settings -> SDKs within the Class Path Tab!
 * Otherwise the IntelliJ Idea doesn't recognise the newly add jfxrt.jar!
 */
fun main(args: Array<String>) {
    launch<MyApp>(args)
}

class MyApp: App(MyView::class)

class MyView : View() {
    override val root = vbox {
        button("Press me")
        label("Waiting")
    }
}