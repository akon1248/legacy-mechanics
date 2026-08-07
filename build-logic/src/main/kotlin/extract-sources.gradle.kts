import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@CacheableTask
abstract class ExtractSourcesTask @Inject constructor(
    private val fileSystem: FileSystemOperations,
    private val archives: ArchiveOperations,
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val paperSourceArchives: ConfigurableFileCollection

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mappedServerJar: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun extract() {
        val destination = outputDirectory.get().asFile
        fileSystem.delete { delete(destination) }
        destination.mkdirs()
        destination.resolve(".gitignore").writeText("*\n")

        fileSystem.copy {
            from(paperSourceArchives.files.map(archives::zipTree))
            mappedServerJar.asFile.orNull?.takeIf { it.isFile }?.let { from(archives.zipTree(it)) }
            into(destination)
            include("**/*.java")
            includeEmptyDirs = false
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val paperApiVersion = providers.gradleProperty("paperApiVersion")
    .orElse(libs.findVersion("purpur").get().requiredVersion)
    .get()
val paperApiSources = configurations.create("paperApiSources") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies.add(paperApiSources.name, "io.papermc.paper:paper-api:$paperApiVersion:sources")

tasks.register<ExtractSourcesTask>("extractSources") {
    group = "build setup"
    dependsOn("paperweightUserdevSetup")
    paperSourceArchives.from(paperApiSources)
    mappedServerJar.set(layout.projectDirectory.file(".gradle/caches/paperweight/taskCache/mappedServerJar.jar"))
    outputDirectory.set(layout.projectDirectory.dir("extracted_sources"))
}
