package ingsist.snippet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SnippetApplication {

	fun main(args: Array<String>) {
		runApplication<SnippetApplication>(*args)
	}

}