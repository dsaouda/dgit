package com.github.dsaouda.dgit

import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.RepositoryService
import java.io.File
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {

    val username = args[0]
    val password = args[1]
    val path = File(args[2])

    if (!path.isDirectory) path.mkdirs()

    val client = GitHubClient()
    client.setCredentials(username, password)

    val repo = RepositoryService(client)
    repo.getRepositories().forEach { r ->
        val cloneUrl = r.cloneUrl.replace("https://", "https://${username}:${password}@")
        val command = "git clone ${cloneUrl} ${path.absolutePath}/${r.name}"

        println("projeto ${r.name} - ${r.cloneUrl}")
        println("${command}")
        bash(command)

        println("")
    }

}

private fun bash(command: String) {
    val p = Runtime.getRuntime().exec(command)
    p.waitFor(5, TimeUnit.MINUTES)
}

