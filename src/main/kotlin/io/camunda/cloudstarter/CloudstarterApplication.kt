package io.camunda.cloudstarter

import io.zeebe.client.api.response.ActivatedJob
import io.zeebe.client.api.worker.JobClient
import io.zeebe.spring.client.EnableZeebeClient
import io.zeebe.spring.client.ZeebeClientLifecycle
import io.zeebe.spring.client.annotation.ZeebeDeployment
import io.zeebe.spring.client.annotation.ZeebeWorker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate




@SpringBootApplication
@EnableZeebeClient
@RestController
@ZeebeDeployment(classPathResources = ["test-process.bpmn"])
class CloudstarterApplication {

	@Autowired
	private val client: ZeebeClientLifecycle? = null

	var logger: Logger = LoggerFactory.getLogger(javaClass)

	@ZeebeWorker(type = "get-time")
	fun handleGetTime(client: JobClient, job: ActivatedJob) {
		val uri = "https://json-api.joshwulf.com/time"

		val restTemplate = RestTemplate()
		val result = restTemplate.getForObject(uri, String::class.java)!!

		client.newCompleteCommand(job.key)
				.variables("{\"time\":$result}")
				.send().join()
	}

	@ZeebeWorker(type = "make-greeting")
	fun handleMakeGreeting(client: JobClient, job: ActivatedJob) {
		val headers = job.customHeaders
		val greeting = headers.getOrDefault("greeting", "Good day")
		val variablesAsMap = job.variablesAsMap
		val name = variablesAsMap.getOrDefault("name", "there") as String
		val say = "$greeting $name"
		client.newCompleteCommand(job.key)
				.variables("{\"say\": \"$say\"}")
				.send().join()
	}
	@GetMapping("/status")
	fun getStatus(): String? {
		val topology = client!!.newTopologyRequest().send().join()
		return topology.toString()
	}

	@GetMapping("/start")
	fun startWorkflowInstance(): String? {
		val workflowInstanceResult = client!!
				.newCreateInstanceCommand()
				.bpmnProcessId("test-process")
				.latestVersion()
				.variables("{\"name\": \"Josh Wulf\"}")
				.withResult()
				.send()
				.join()
		return workflowInstanceResult
				.variablesAsMap
				.getOrDefault("say", "Error: No greeting returned") as String?
	}

	fun main(args: Array<String>) {
		runApplication<CloudstarterApplication>(*args)
	}
}
