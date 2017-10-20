import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets

class FileMergeTask extends DefaultTask {

    @InputFiles
    FileCollection inputFiles

    @OutputFile
    File target

    @TaskAction
    void mergeFiles() {
        Set<String> lines = new TreeSet<>() //keep entries alphabetized
        inputFiles.each { lines.addAll(it.readLines()) }
        target.write(String.join('\n', lines), StandardCharsets.UTF_8.name())
    }
}