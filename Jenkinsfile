pipeline {
  agent any
  tools {
    jdk 'temurin-jdk21-latest'
  }
  environment {
    MAVEN_HOME = "$WORKSPACE/.m2/"
    MAVEN_USER_HOME = "$MAVEN_HOME"
  }
  stages {
    stage("Build LSP4MP JDT.LS extension"){
      steps {
        withMaven {
          sh 'cd microprofile.jdt && ./mvnw clean verify -B  -Peclipse-sign && cd ..'
        }
      }
    }
    stage('Deploy to downloads.eclipse.org') {
      when {
        branch 'master'
      }
      steps {
        sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
          sh '''
            VERSION=`grep -o '[0-9].*[0-9]' microprofile.jdt/org.eclipse.lsp4mp.jdt.core/target/maven-archiver/pom.properties`
            targetDir=/home/data/httpd/download.eclipse.org/lsp4mp/snapshots/$VERSION
            ssh genie.lsp4mp@projects-storage.eclipse.org rm -rf $targetDir
            ssh genie.lsp4mp@projects-storage.eclipse.org mkdir -p $targetDir
            scp -r microprofile.jdt/org.eclipse.lsp4mp.jdt.site/target/*.zip genie.lsp4mp@projects-storage.eclipse.org:$targetDir
            ssh genie.lsp4mp@projects-storage.eclipse.org unzip $targetDir/*.zip -d $targetDir/repository
            '''
        }
      }
    }
    stage("Build LSP4MP Language Server"){
      steps {
        withMaven {
          sh 'cd microprofile.ls/org.eclipse.lsp4mp.ls && ./mvnw clean verify -B -Dcbi.jarsigner.skip=false && cd ../..'
        }
      }
    }
    stage ('Deploy LSP4MP Language Server artifacts to Maven repository') {
      when {
          branch 'master'
      }
      steps {
        withMaven {
          sh 'cd microprofile.ls/org.eclipse.lsp4mp.ls && ./mvnw deploy -B -DskipTests'
        }
      }
    }
  }
  post {
    always {
      junit '**/target/surefire-reports/*.xml'
    }
  }
}
