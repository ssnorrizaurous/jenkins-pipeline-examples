package org.common

def test1() {
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
}