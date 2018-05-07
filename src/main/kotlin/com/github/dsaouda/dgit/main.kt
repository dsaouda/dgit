package com.github.dsaouda.dgit

import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import java.io.File
import java.util.concurrent.TimeUnit

private val params = listOf(
    "-u=<informe o username>",
    "-p=<informe o password>",
    "-d=<informe o diretório aonde os projetos deverão ser salvos>"
)

fun main(args: Array<String>) {
    try {
        run(args)
    } catch (e: NullPointerException) {
        mostrarModoDeUso()
    } catch (e: Exception) {
        println("Ocorreu um problema")
        println("-------------------")
        e.printStackTrace()
        println("-------------------")
    }
}

private fun run(args: Array<String>) {
    val parse = parserArgs(args)
    val username = parse["-u"]
    val password = parse["-p"]
    val path = File(parse["-d"])

    println("iniciando o processamento")
    println("usuario: ${username}")
    println("password: ${password}")

    criarDiretorio(path)
    val repos = repositorios(username!!, password!!)


    repos.forEachIndexed({ i, r ->
        println("${i.plus(1)} de ${repos.size}")

        var workdir = File("${path.absolutePath}/${r.name}")

        var command: String = ""

        println("projeto ${r.name} - ${r.cloneUrl}")

        //tentativas para executar o comando com sucesso
        for (index in 1..3) {

            when (gitexists(workdir)) {
                true -> command = gitpull()
                false -> {
                    workdir = workdir.parentFile
                    val giturl = giturl(r.cloneUrl, username, password)
                    command = gitclone(giturl)
                }
            }

            println("${command}")
            val saidaGit = runCommand(command, workdir)

            if (saidaGit.contains("Checking connectivity... done", true) ||
                saidaGit.contains("Already up-to-date", true) ||
                saidaGit.contains("Resolving deltas:    100%", true) ||
                (saidaGit.contains("Updating", true) && saidaGit.contains("Fast-forward", true))
            ) {
                print(" [ok]")
                println("\n----------------------")
                break
            }

            println("[error] ${index} de 3 tentativas ... ")
        }
    })
}

private fun gitexists(workdir: File) = workdir.exists()
private fun gitpull() = "git pull --all -p"
private fun gitclone(cloneUrl: String) = "git clone -v --progress ${cloneUrl}"

private fun giturl(cloneUrl: String, username: String?, password: String?): String {
    val passwordUrl = password?.replace("@", "%40")
    val url = "https://${username}:${passwordUrl}@"
    return cloneUrl.replace("https://", url)
}

private fun repositorios(username: String, password: String): List<Repository> {
    val client = GitHubClient()
    client.setCredentials(username, password)

    val repo = RepositoryService(client)
    val repos = repo.getRepositories()
    return repos
}

private fun criarDiretorio(path: File) { if (!path.isDirectory) path.mkdirs() }

private fun parserArgs(args: Array<String>): Map<String, String> {

    val commands = mutableMapOf<String, String>()
    args.forEach { a ->
        val valor = a.split("=")
        commands.put(valor[0], valor[1])
    }

    return commands
}

private fun mostrarModoDeUso() {
    println("Modo de uso")
    println(params.joinToString("\n"))
    println("-------")
    println("exemplo")
    println("java -jar dgit.jar -u=usuario -p=senha -d=/home/ubuntu/github")
    println("")
}

private fun runCommand(command: String, workingDir: File): String {
    val commands = command.split("\\s".toRegex())
    val proc = ProcessBuilder(*commands.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

    while(proc.isAlive) {
        print(".")
        Thread.sleep(1000)
    }

    return proc.errorStream.bufferedReader().readText() + proc.inputStream.bufferedReader().readText()
    //return proc.inputStream.bufferedReader().readText()
}