node {
    stage('Checkout') {
        sh 'rm -rf * .git'
        checkout scm
    }
    stage('Build') {
        sh 'make'
        //for windows
        bat 'make'

        //save artifacts from build to master for later retrival - not a sub for nexus/artifactory
        archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
    }

    stage('Test') {
        sh 'make check'
        junit 'reports/**/*.xml'
    }

    // Make sure if there were any test failures
    stage('Deploy') {
        if (currentBuild.result == null || currentBuild.result == 'SUCCESS') { (1)
            sh 'make publish'
        }
    }
}

// set to only run on master or node that has label?
node('master') {
    stage('Hello'){
        echo 'Hello World'
    }
}

// build to label
node (label = 'example-label') {
    stage('Hello'){
        echo 'Hello World'
    }
}

// Flow control with if/else
node {
    stage('Example') {
        if (env.BRANCH_NAME == 'master') {
            echo 'I only execute on the master branch'
        } else {
            echo 'I execute elsewhere'
        }
    }
}

//exception handling (try/catch/finally)
node {
    stage('Example') {
        try {
            sh 'exit 1'
        }
        catch (exc) {
            echo 'Something failed, I should sound the klaxons!'
            throw
        }
    }
}

//Basic Maven
node {
    def mvnHome
    stage('Preparation') { // for display purposes
        // Get some code from a GitHub repository
        git 'https://github.com/jglick/simple-maven-project-with-tests.git'
        // Get the Maven tool.
        // ** NOTE: This 'M3' Maven tool must be configured
        // **       in the global configuration.
        mvnHome = tool 'M3'
    }
    stage('Build') {
        // Run the maven build
        if (isUnix()) {
            sh "'${mvnHome}/bin/mvn' -Dmaven.test.failure.ignore clean package"
        } else {
            bat(/"${mvnHome}\bin\mvn" -Dmaven.test.failure.ignore clean package/)
        }
    }
    stage('Results') {
        junit '**/target/surefire-reports/TEST-*.xml'
        archive 'target/*.jar'
    }
}

// def a variable
def singleQuoted = 'Hello'
def doubleQuoted = "Hello"

def thingA = "bananas"
node {
    stage('Hello'){
        def thingB = "apples"
        echo "I like ${thingA} and ${thingB}"
    }

    stage('Goodbye'){
        echo "I like ${thingA}"
    }
}

// global variable env "env" which is available from anywhere within a Jenkinsfile
node {
    stage('EnvTest'){
        echo "${env.JOB_NAME}-${env.BUILD_ID}"
    }
}

node {
    echo "Running ${env.BUILD_ID} on ${env.JENKINS_URL}"
}

// setting env variables dynamically in a step to be used elsewhere
node {
    stage('set'){
        env.test = 'See_ME?'
        echo "${env.test}"
    }

    stage('get'){
        echo "${env.test}"
        echo "${test}"
    }
}

// setting env variable
node {
    withEnv(["PATH+MAVEN=${tool 'M3'}/bin"]) {
        sh 'mvn -B verify'
    }
}

// refer to current build
node {
    stage('start'){
        sh 'echo "test"'
    }
    stage('finish'){
        echo "${currentBuild.currentResult}"
    }
}

// set variable to return of a command
def status = sh(returnStatus: true, script: "git merge --no-edit $branches > merge_output.txt")

// run things in parallel - must be in single stage
node {
    stage('parallel-test') {
        parallel (
            'test1': {
                sh 'echo test1'
            },
            'test2': {
                sh 'echo test2'
            }
                )
    }
}

// To cleanup after failure, wrap in try/catch
node {
    stage('Execute some shell') {
        try {
            sh 'dne'
        }catch (e) {
            sh './post_build_failure_script'
            throw e
        }
    }
}


// stash
stage('Build') {
    node {
        checkout scm
        sh 'make'
        stash includes: '**/target/*.jar', name: 'app'
    }
}

stage('Test') {
    node('linux') {
        checkout scm
        try {
            unstash 'app'
            sh 'make check'
        }
        finally {
            junit '**/target/*.xml'
        }
    }
    node('windows') {
        checkout scm
        try {
            unstash 'app'
            bat 'make check'
        }
        finally {
            junit '**/target/*.xml'
        }
    }
}