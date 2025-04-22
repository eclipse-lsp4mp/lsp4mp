pipeline {
  agent any
  tools {
    jdk 'temurin-jdk21-latest'
  }
  environment {
    MAVEN_HOME = "$WORKSPACE/.m2/"
    MAVEN_USER_HOME = "$MAVEN_HOME"
  }
  parameters {
      string(name: 'VERSION', defaultValue: '', description: 'Version to Release?')
  }
  stages {
    stage("Release LSP4MP Language Server"){
      steps {
        script {
          if (!params.VERSION) {
            error('Not releasing')
          }
        }
        withMaven {
          sh "VERSION=${params.VERSION}"
          sh '''
                cd microprofile.ls/org.eclipse.lsp4mp.ls
                ./mvnw versions:set -DnewVersion=$VERSION
                ./mvnw versions:set-scm-tag -DnewTag=$VERSION
                ./mvnw clean deploy -B -Peclipse-sign -Dcbi.jarsigner.skip=false

                cd ../../microprofile.jdt
                ./mvnw org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=$VERSION-SNAPSHOT
                ./mvnw versions:set-scm-tag -DnewTag=$VERSION
                ./mvnw clean verify -B  -Peclipse-sign -DskipTests
                cd ..
              '''
        }
      }
    }

    stage('Deploy to downloads.eclipse.org') {
      steps {
        sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
          sh "VERSION=${params.VERSION}"
          sh '''
            targetDir=/home/data/httpd/download.eclipse.org/lsp4mp/releases/$VERSION
            ssh genie.lsp4mp@projects-storage.eclipse.org rm -rf $targetDir
            ssh genie.lsp4mp@projects-storage.eclipse.org mkdir -p $targetDir
            scp -r microprofile.jdt/org.eclipse.lsp4mp.jdt.site/target/*.zip genie.lsp4mp@projects-storage.eclipse.org:$targetDir
            ssh genie.lsp4mp@projects-storage.eclipse.org unzip $targetDir/*.zip -d $targetDir/repository
            '''
        }
      }
    }

    stage('Push tag to git') {
      steps {
        sshagent ( ['github-bot-ssh']) {
          sh "VERSION=${params.VERSION}"
          sh '''
            git config --global user.email "lsp4mp-bot@eclipse.org"
            git config --global user.name "LSP4MP GitHub Bot"
            git add "**/pom.xml" "**/MANIFEST.MF"
            git commit -sm "Release $VERSION"
            git tag $VERSION
            git push origin $VERSION
          '''
        }
      }
    }
  }
}
