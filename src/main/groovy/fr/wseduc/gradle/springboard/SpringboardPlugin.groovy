package fr.wseduc.gradle.springboard

import groovy.io.FileType
import java.io.*;
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.process.ExecResult
import java.util.zip.*

class SpringboardPlugin implements Plugin<Project> {

	@Override
	void apply(Project project) {
		project.task("generateConf") << {
			FileUtils.createFile("conf.properties", "ent-core.json.template", "ent-core.json")
		}

		project.task("generateTestConf") << {
			FileUtils.createFile("test.properties", "ent-core.json.template", "ent-core.embedded.json")
		}

		project.task("extractDeployments") << {
			extractDeployments(project)
		}

		project.task("extractTheme") << {
			extractTheme(project)
		}

		project.task("init") << {
			extractDeployments(project)
			initFiles(project)
		}

		project.task(dependsOn: ['runEnt', 'compileTestScala'], "integrationTest") << {
			gatling(project)
			stopEnt(project)
		}

		project.task(dependsOn: 'generateTestConf', "runEnt") << {
			runEnt(project)
		}

		project.task("stopEnt") << {
			stopEnt(project)
		}

	}

	private ExecResult runEnt(Project project) {
		project.exec {
			workingDir '.'

			if (System.env.OS != null && System.env.OS.contains('Windows')) {
				commandLine 'cmd', '/c', 'run.bat'
			} else {
				commandLine './run.sh'
			}
		}
	}

	private ExecResult stopEnt(Project project) {
		project.exec {
			workingDir '.'

			if (System.env.OS != null && System.env.OS.contains('Windows')) {
				commandLine 'cmd', '/c', 'stop.bat'
			} else {
				commandLine './stop.sh'
			}
		}
	}

	private void gatling(Project project) {
		def simulations = new File(project.sourceSets.test.output.classesDir.getPath() + File.separator + 'org' + File.separator + 'entcore' + File.separator + 'test' + File.separator + 'simulations')

		project.logger.lifecycle(" ---- Executing all Gatling scenarios from: ${simulations} ----")
		simulations.eachFileRecurse { file ->
			if (file.isFile()) {
				//Remove the full path, .class and replace / with a .
				project.logger.debug("Tranformed file ${file} into")
				def gatlingScenarioClass = (file.getPath() - (project.sourceSets.test.output.classesDir.getPath() + File.separator) - '.class')
						.replace(File.separator, '.')

				project.logger.debug("Tranformed file ${file} into scenario class ${gatlingScenarioClass}")
				System.setProperty("gatling.http.connectionTimeout", "300000")
				project.javaexec {
					main = 'io.gatling.app.Gatling'
					classpath = project.sourceSets.test.output + project.sourceSets.test.runtimeClasspath
					args '-rbf',
							project.sourceSets.test.output.classesDir,
							'-s',
							gatlingScenarioClass,
							'-rf',
							'build/reports/gatling'
				}
			}
		}
		project.logger.lifecycle(" ---- Done executing all Gatling scenarios ----")
	}

	private void extractTheme(Project project){
		project.copy {
			from "deployments/assets/themes"
			into "assets/themes"
		}
	}

	private void extractDeployments(Project project) {
		if (!project.file("deployments")?.exists()) {
			project.file("deployments").mkdir()
		}
		project.copy {
			from {
				project.configurations.deployment.collect { project.zipTree(it) }
			}
			into "deployments/"
		}
		project.file("deployments/org")?.deleteDir()
		project.file("deployments/com")?.deleteDir()
		project.file("deployments/fr")?.deleteDir()
		project.file("deployments/errors")?.deleteDir()
		project.file("deployments/META-INF")?.deleteDir()
		project.file("deployments/org")?.deleteDir()
		project.file("deployments/git-hash")?.delete()
	}

	def initFiles(Project project) {
		File runsh = project.file("run.sh")
		File runbat = project.file("run.bat")
		File stopsh = project.file("stop.sh")
		File stopbat = project.file("stop.bat")
		stopbat.write("wmic process where \"name like '%%java%%'\" delete")
		stopsh.write(
				"#!/bin/sh\n" +
				"docker-compose stop neo4j\n" +
				"PID_ENT=\$(ps -ef | grep \"org.entcore~infra\" | grep -v grep | sed 's/\\s\\+/ /g' | cut -d' ' -f2)\n" +
				"kill \$PID_ENT"
		)
		String version = project.getProperties().get("entCoreVersion")
		runbat.write(
						"vertx runMod org.entcore~infra~" + version + " -conf ent-core.embedded.json"
		)
		runsh.write(
				"#!/bin/bash\n" +
				"docker-compose up -d neo4j > /dev/null &\n" +
				"sleep 10\n" +
				"vertx runMod org.entcore~infra~" + version + " -conf ent-core.embedded.json > /dev/null &"
		)

		runsh.setExecutable(true, true)
		stopsh.setExecutable(true, true)

		project.file("sample-be1d/Ecole primaire Emile Zola")?.mkdirs()
		project.file("neo4j-conf")?.mkdirs()
		project.file("src/test/scala/org/entcore/test/scenarios")?.mkdirs()
		project.file("src/test/scala/org/entcore/test/simulations")?.mkdir()

		File scn = project.file("src/test/scala/org/entcore/test/scenarios/IntegrationTestScenario.scala")
		InputStream scnis = this.getClass().getClassLoader()
				.getResourceAsStream("src/test/scala/org/entcore/test/scenarios/IntegrationTestScenario.scala")
		FileUtils.copy(scnis, scn)

		File sim = project.file("src/test/scala/org/entcore/test/simulations/IntegrationTest.scala")
		InputStream simis = this.getClass().getClassLoader()
				.getResourceAsStream("src/test/scala/org/entcore/test/simulations/IntegrationTest.scala")
		FileUtils.copy(simis, sim)

		File i0 = project.file("sample-be1d/Ecole primaire Emile Zola/CSVExtraction-eleves.csv")
		File i1 = project.file("sample-be1d/Ecole primaire Emile Zola/CSVExtraction-enseignants.csv")
		File i2 = project.file("sample-be1d/Ecole primaire Emile Zola/CSVExtraction-responsables.csv")
		InputStream is0 = this.getClass().getClassLoader()
				.getResourceAsStream("sample-be1d/Ecole primaire Emile Zola/CSVExtraction-eleves.csv")
		InputStream is1 = this.getClass().getClassLoader()
				.getResourceAsStream("sample-be1d/Ecole primaire Emile Zola/CSVExtraction-enseignants.csv")
		InputStream is2 = this.getClass().getClassLoader()
				.getResourceAsStream("sample-be1d/Ecole primaire Emile Zola/CSVExtraction-responsables.csv")
		FileUtils.copy(is0, i0)
		FileUtils.copy(is1, i1)
		FileUtils.copy(is2, i2)

		File neo4jConf = project.file("neo4j-conf/neo4j.conf")
		InputStream neo4jConfStream = this.getClass().getClassLoader()
				.getResourceAsStream("neo4j-conf/neo4j.conf")
		FileUtils.copy(neo4jConfStream, neo4jConf)

		File dockerCompose = project.file("docker-compose.yml")
		InputStream dockerComposeStream = this.getClass().getClassLoader()
				.getResourceAsStream("docker-compose.yml")
		FileUtils.copy(dockerComposeStream, dockerCompose)

		File gulpfile = project.file("gulpfile.js")
		InputStream gulpfileStream = this.getClass().getClassLoader()
				.getResourceAsStream("gulpfile.js")
		FileUtils.copy(gulpfileStream, gulpfile)

		File packageJson = project.file("package.json")
		InputStream packageJsonStream = this.getClass().getClassLoader()
				.getResourceAsStream("package.json")
		FileUtils.copy(packageJsonStream, packageJson)


		File entcoreJsonTemplate = project.file("ent-core.json.template")
		FileUtils.copy(this.getClass().getClassLoader().getResourceAsStream("ent-core.json.template"),
				entcoreJsonTemplate)

		String filename = "conf.properties"
		File confProperties = project.file(filename)
		Map confMap = FileUtils.createOrAppendProperties(confProperties, filename)

		String filenameTest = "test.properties"
		File testProperties = project.file(filenameTest)
		Map testMap = FileUtils.createOrAppendProperties(testProperties, filenameTest)

		String filenameDefault = "default.properties"
		File defaultProperties = project.file(filenameDefault)
		Map defaultMap = FileUtils.createOrAppendProperties(defaultProperties, filenameDefault)

		Map appliPort = [:]
		project.file("deployments").eachDir {
			it.eachDir { dir ->
				String dest = "migration".equals(dir.name) ? dir.name + File.separator + it.name : dir.name
				new AntBuilder().copy( todir: dest ) {
					fileset( dir: dir.absolutePath )
				}
			}
			it.eachFile(FileType.FILES) { file ->
				File f
				switch (file.name) {
					case "conf.json.template":
						f = entcoreJsonTemplate
						f.append(",\n")
						f.append(file.text)
						file.eachLine { line ->
							def matcher = line =~ /\s*\t*\s*"port"\s*:\s*([0-9]+)[,]?\s*\t*\s*/
							if (matcher.find()) {
								appliPort.put(it.name, matcher.group(1))
							}
						}
						break;
					case "test.scala":
						f = scn
						f.append("\n" + file.text)
						break;
					case "conf.properties" :
						FileUtils.appendProperties(project, file, confMap)
						break;
					case "test.properties" :
						FileUtils.appendProperties(project, file, testMap)
						break;
					case "default.properties" :
						FileUtils.appendProperties(project, file, defaultMap)
						break;
				}
			}
		}

		InputStream httpProxy = this.getClass().getClassLoader().getResourceAsStream("http-proxy.json.template")
		entcoreJsonTemplate.append(httpProxy.text)
		appliPort.each { k, v ->
			entcoreJsonTemplate.append(
					",\n" +
					"          {\n" +
					"            \"location\": \"/" + k + "\",\n" +
					"            \"proxy_pass\": \"http://localhost:" + v + "\"\n" +
					"          }"
			)
		}
		entcoreJsonTemplate.append(
				"        ]\n" +
				"      }\n" +
				"    }\n" +
				"<% } %>" +
				"  ]\n" +
				"}"
		)
		scn.append("\n}")

		if (!confMap.containsKey("entcoreVersion")) {
			confProperties.append("entcoreVersion=" + version + "\n")
		}
		if (!testMap.containsKey("entcoreVersion")) {
			testProperties.append("entcoreVersion=" + version + "\n")
		}
	}

}