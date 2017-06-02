/*
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.apache.ivy.plugins.resolver.FileSystemResolver
import org.apache.ivy.plugins.resolver.URLResolver

grails.project.work.dir = 'work'
grails.project.class.dir = 'target/classes'
grails.project.test.class.dir = 'target/test-classes'
grails.project.test.reports.dir = 'target/test-reports'
grails.project.war.file = "target/${appName}.war"

grails.project.dependency.resolver = "maven"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()

        // Optional custom repository for dependencies.
        Closure internalRepo = {
            String repoUrl = 'http://artifacts/ext-releases-local'
            String artifactPattern = '[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]'
            String ivyPattern = '[organisation]/[module]/[revision]/[module]-[revision]-ivy.[ext]'
            URLResolver urlLibResolver = new URLResolver()
            urlLibResolver.with {
                name = repoUrl
                addArtifactPattern("${repoUrl}/${artifactPattern}")
                addIvyPattern("${repoUrl}/${ivyPattern}")
                m2compatible = true
            }
            resolver urlLibResolver

            String localDir = System.getenv('IVY_LOCAL_REPO') ?: "${System.getProperty('user.home')}/ivy2-local"
            FileSystemResolver localLibResolver = new FileSystemResolver()
            localLibResolver.with {
                name = localDir
                addArtifactPattern("${localDir}/${artifactPattern}")
                addIvyPattern("${localDir}/${ivyPattern}")
            }
            resolver localLibResolver
        }
        // Comment or uncomment the next line to toggle the use of an internal artifacts repository.
        //internalRepo()
    }

    dependencies {

        compile(
                // Amazon Web Services programmatic interface
                'com.amazonaws:aws-java-sdk:1.11.136',
                // Transitive dependencies of aws-java-sdk, but also used directly.
                // It would be great if we could upgrade httpcore and httpclient, but we can't until the AWS Java SDK
                // upgrades its dependencies. If we simply upgrade these, then some Amazon calls fail.
                'org.apache.httpcomponents:httpcore:4.4.4',
                'org.apache.httpcomponents:httpclient:4.5.2',

                // Explicitly including aws-java-sdk transitive dependencies
                'org.codehaus.jackson:jackson-core-asl:1.8.9',
                'org.codehaus.jackson:jackson-mapper-asl:1.8.9',

                // Extra collection types and utilities
                'commons-collections:commons-collections:3.2.1',

                // Easier Java from of the Apache Foundation
                'commons-lang:commons-lang:2.4',
     
                // Better Zip Support
                'org.apache.commons:commons-compress:1.8',
                
                // Better IO Support
                'commons-io:commons-io:2.4',

                // Easier Java from Joshua Bloch and Google
                'com.google.guava:guava:14.0',

                // Send emails about system errors and task completions
                'javax.mail:mail:1.4.1',

                // Better date API
                'joda-time:joda-time:2.0',

                'net.sourceforge.javacsv:javacsv:2.0',

                'org.apache.poi:poi-ooxml:3.7',
                'org.codehaus.woodstox:wstx-asl:3.2.9',
                'jfree:jfreechart:1.0.13',
                'org.json:json:20090211',
                'org.mapdb:mapdb:0.9.1',

                // Since the AWS SDK has removed its package "com.amazonaws.util.json", we need to include it as a separate library 
                'org.json:json:20090211'

        ) { // Exclude superfluous and dangerous transitive dependencies
            excludes(
                    // Some libraries bring older versions of JUnit as a transitive dependency and that can interfere
                    // with Grails' built in JUnit
                    'junit',

                    'mockito-core',
            )
        }
    }

    plugins {
		//runtime ":hibernate4:4.3.6.1"
		build ":tomcat:8.0.20"
    }
}
