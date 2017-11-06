@file:JvmName("Generator")

package edu.sharif.cs.contests

import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


/**
 * Specifies initialization function.
 * Function must be public and must not declare any parameters.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Init

/**
 * Specifies sample input function.
 * Function must be public and must declare one parameter of type java.io.PrintWriter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SampleInput

/**
 * Specifies input generator function.
 * Function must be public and must declare two parameters of types java.io.PrintWriter and java.util.Random.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class InputTest

/**
 * Specifies output generator function.
 * Function must be public and must declare two parameters of types java.util.Scanner and java.io.PrintWriter.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OutputTest

/**
 * Specifies clean-up function.
 * Function must be public and must not declare any parameters.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Destroy

/**
 * Finds a method that has the specified annotation in the given class.
 * Method must be public and if multiple methods are present one of them is returned.
 */
private fun findMethod(cls: Class<*>, annotation: Class<out Annotation>): Method =
        cls.methods
                .first { it.getAnnotation(annotation) != null }

/**
 * Returns all methods that are annotated as sample inputs.
 */
private fun allSamples(cls: Class<*>): List<Method> =
        cls.methods
                .filter { it.getAnnotation(SampleInput::class.java) != null }

/**
 * Invokes the function which is annotated as Init.
 */
fun <T : Any> T.init(): T {
    val method = findMethod(this::class.java, Init::class.java)
    method.invoke(this)
    return this
}

/**
 * Invokes the function which is annotated as input generator.
 * This function should accept two parameters: a java.io.PrintWriter and a java.util.Random
 */
fun <T : Any> T.input(writer: PrintWriter, randomGenerator: Random): T {
    val method = findMethod(this::class.java, InputTest::class.java)
    method.invoke(this, writer, randomGenerator)
    writer.flush()
    writer.close()
    return this
}

/**
 * Invokes the function which is annotated as output generator.
 * This function should accept two parameters: a java.util.Scanner and a java.io.PrintWriter
 */
fun <T : Any> T.output(reader: Scanner, writer: PrintWriter): T {
    val method = findMethod(this::class.java, OutputTest::class.java)
    method.invoke(this, reader, writer)
    reader.close()
    writer.flush()
    writer.close()
    return this
}

/**
 * Invokes the function which is annotated as clean-up.
 */
fun <T : Any> T.destroy() {
    val method = findMethod(this::class.java, Destroy::class.java)
    method.invoke(this)
}

/**
 * Static random generator is passed to input generator each time.
 */
private val _ranGenerator = Random()

/**
 * This function generates input and output test cases.
 * First, it makes a new instance and initiates it.
 * Then makes directories for input and output test files and deletes any previously present files.
 * Then it generates sample cases and fills the rest with input.
 * After that, it generates output test cases.
 * At last destroy method is called and all is finished.
 */
@JvmOverloads
fun generate(testClass: Class<*>, num: Int = 40, path: String = "test-cases/${testClass.simpleName.toLowerCase()}/", log: PrintStream = System.out) {
    log.println("Starting test case generation...")
    log.println("Instantiating test class...")
    val instance = testClass.getConstructor().newInstance()
    try {
        instance.init()
    } catch (e: NoSuchElementException) {
    }
    log.println("Test class initiated.")

    val baseDir = File(path)
    if (baseDir.exists()) {
        log.println("Deleting old files from: '${baseDir.absolutePath}'...")
        baseDir.deleteRecursively()
        log.println("Deleted.")
    }

    val inputDir = File(path, "in")
    val outputDir = File(path, "out")
    inputDir.mkdirs()
    outputDir.mkdirs()
    log.println("Input test case directory: '${inputDir.absolutePath}'")
    log.println("Output test case directory: '${outputDir.absolutePath}'")

    log.println("Starting to generate sample inputs...")
    val inCount = AtomicInteger(1)
    allSamples(instance::class.java)
            .forEach {
                if (inCount.get() <= num) {
                    val writer = PrintWriter(File(inputDir, "input${inCount.getAndIncrement()}.txt"))
                    it.invoke(instance, writer)
                    writer.flush()
                    writer.close()
                    log.println("Sample test #${inCount.get() - 1} written to file.")
                }
            }
    log.println("Samples finished.")
    log.println("Starting to generate extra inputs...")
    var maxInTime = 0L
    while (inCount.get() <= num)
        try {
            val startTime = System.currentTimeMillis()
            instance.input(
                    PrintWriter(File(inputDir, "input${inCount.getAndIncrement()}.txt")),
                    _ranGenerator
            )
            val timeElapsed = System.currentTimeMillis() - startTime
            if (timeElapsed > maxInTime) maxInTime = timeElapsed
            log.println("Input test #${inCount.get() - 1} generated and written to file. Time elapsed: $timeElapsed ms.")
        } catch (e: NoSuchElementException) {
        }
    log.println("Maximum time for input generation: $maxInTime ms.")

    log.println("-------------------------------------------------")
    log.println()

    log.println("Starting to generate outputs...")
    var maxOutTime = 0L
    for (outCount in 1..num)
        try {
            val startTime = System.currentTimeMillis()
            instance.output(
                    Scanner(File(inputDir, "input$outCount.txt")),
                    PrintWriter(File(outputDir, "output$outCount.txt"))
            )
            val timeElapsed = System.currentTimeMillis() - startTime
            if (timeElapsed > maxOutTime) maxOutTime = timeElapsed
            log.println("Output test #$outCount generated and written to file. Time elapsed: $timeElapsed ms.")
        } catch (e: NoSuchElementException) {
        }
    log.println("Maximum time for output generation: $maxOutTime ms.")

    log.println("-------------------------------------------------")
    log.println()

    log.println("All finished. Exiting...")
    try {
        instance.destroy()
    } catch (e: NoSuchElementException) {
    }
}

/**
 * Prints help text.
 */
private fun printHelp() {
    println("Usage: generate TEST_CLASS [OUTPUT_DIR] [TEST_COUNT].")
    println()
}

/**
 * A default main method to help quickly generate test cases.
 */
fun main(args: Array<String>) {
    when (args.size) {
        1 -> try {
            generate(Class.forName(args[0]))
        } catch (e: Exception) {
            printHelp()
            println("Error occurred: ${e.message}")
        }
        2 -> try {
            try {
                generate(Class.forName(args[0]), num = Integer.parseInt(args[1]))
            } catch (e: NumberFormatException) {
                generate(Class.forName(args[0]), path = args[1])
            }
        } catch (e: Exception) {
            printHelp()
            println("Error occurred: ${e.message}")
        }
        3 -> try {
            generate(Class.forName(args[0]), num = Integer.parseInt(args[2]), path = args[1])
        } catch (e: Exception) {
            printHelp()
            println("Error occurred: ${e.message}")
        }
        else -> {
            printHelp()
        }
    }
}
