// ============================================================
// Jenkinsfile — CI/CD Pipeline for Notes App Automation Suite
// ============================================================
// WHY A DECLARATIVE PIPELINE (not scripted):
// Declarative syntax is the modern Jenkins standard. It's more readable,
// enforces structure, and integrates better with Blue Ocean UI. The
// 'pipeline {}' block is parsed and validated before execution starts,
// catching syntax errors early.
//
// AI DEFECT TRIAGE INTEGRATION:
// This pipeline is now configured to support Groq AI Automated Defect Analysis.
// The AI API key is securely injected into the test execution via Jenkins Credentials
// to ensure enterprise-grade security (no hardcoded keys in version control).
// ============================================================

pipeline {

    agent any
    // WHY 'any': Runs on any available agent. For production, you'd
    // specify agent { label 'selenium-node' } to run only on machines
    // with browsers installed.

    // ===== ENVIRONMENT VARIABLES =====
    // Centralized config: change once, applies to all stages
    environment {
        JAVA_HOME         = tool 'JDK21'
        MAVEN_HOME        = tool 'Maven3'
        APP_URL           = 'https://practice.expandtesting.com/notes/app/login'
        API_URL           = 'https://practice.expandtesting.com/notes/api'
        BROWSER           = 'chrome'
        HEADLESS          = 'true'
        ALLURE_RESULTS    = 'target/allure-results'
        REPORT_DIR        = 'reports'
        TIMESTAMP         = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date())
        
        // Fetch the AI key securely from the Jenkins Credentials Vault
        GROQ_API_KEY_VAULT = credentials('GROQ_API_KEY')
    }

    // ===== BUILD PARAMETERS =====
    // Allows triggering specific test subsets from Jenkins UI
    parameters {
        choice(name: 'TEST_SUITE',
               choices: ['testng.xml', 'testng-parallel.xml'],
               description: 'Which TestNG suite to execute')
        booleanParam(name: 'RUN_PARALLEL',
                     defaultValue: false,
                     description: 'Run cross-browser parallel suite?')
        string(name: 'BROWSER_OVERRIDE',
               defaultValue: '',
               description: 'Override browser (leave blank to use config)')
    }

    // ===== TRIGGERS =====
    triggers {
        // WHY POLLSCM: Checks Git for changes every 5 minutes.
        // In production, replace with a GitHub webhook for instant triggers.
        pollSCM('H/5 * * * *')
    }

    stages {

        // ----------------------------------------------------------
        stage('Checkout') {
        // PURPOSE: Pulls the latest code from Git repository.
        // WHY EXPLICIT CHECKOUT: Ensures we always test the latest commit,
        // not a stale local copy on the Jenkins agent.
        // ----------------------------------------------------------
            steps {
                echo "========== STAGE: Checkout =========="
                checkout scm
                echo "Branch: ${env.GIT_BRANCH} | Commit: ${env.GIT_COMMIT}"
            }
        }

        // ----------------------------------------------------------
        stage('Environment Verification') {
        // PURPOSE: Verify the agent has required tools before running tests.
        // WHY: Failing fast here saves time vs. discovering a missing tool
        // 20 minutes into test execution.
        // ----------------------------------------------------------
            steps {
                echo "========== STAGE: Environment Verification =========="
                script {
                    if (isUnix()) {
                        sh '''
                            echo "Java version:"
                            java -version

                            echo "Maven version:"
                            mvn -version

                            echo "Chrome version:"
                            google-chrome --version || chromium-browser --version || echo "Chrome not found"

                            echo "ChromeDriver:"
                            chromedriver --version || echo "ChromeDriver will be auto-managed by WebDriverManager"
                        '''
                    } else {
                        bat '''
                            echo Java version:
                            java -version

                            echo Maven version:
                            mvn -version

                            echo Chrome version:
                            reg query "HKEY_CURRENT_USER\\Software\\Google\\Chrome\\BLBeacon" /v version 2>nul || reg query "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome" /v DisplayVersion 2>nul || echo Chrome not found

                            echo ChromeDriver:
                            chromedriver --version 2>nul || echo ChromeDriver will be auto-managed by WebDriverManager
                        '''
                    }
                }
            }
        }

        // ----------------------------------------------------------
        stage('Build & Compile') {
        // PURPOSE: Compile Java source code and download dependencies.
        // WHY SEPARATE FROM TEST: Compilation errors are flagged without
        // spinning up browsers. Fast feedback for developers.
        // -DskipTests skips test execution but still compiles test sources.
        // ----------------------------------------------------------
            steps {
                echo "========== STAGE: Build & Compile =========="
                script {
                    if (isUnix()) {
                        sh 'mvn clean compile test-compile -DskipTests'
                    } else {
                        bat 'mvn clean compile test-compile -DskipTests'
                    }
                }
            }
            post {
                failure {
                    echo "BUILD FAILED: Compilation errors detected. Check console output."
                }
            }
        }

        // ----------------------------------------------------------
        stage('Run Tests') {
        // PURPOSE: Execute the automation suite.
        // ----------------------------------------------------------
            steps {
                echo "========== STAGE: Run Tests =========="
                script {
                    def suite = params.RUN_PARALLEL ? 'testng-parallel.xml' : params.TEST_SUITE
                    def browserFlag = params.BROWSER_OVERRIDE?.trim() ? "-Dbrowser=${params.BROWSER_OVERRIDE}" : "-Dbrowser=${BROWSER}"
                    
                    // Use the vault credential automatically
                    def aiKeyFlag = "-Dai.api.key=${env.GROQ_API_KEY_VAULT}"

                    if (isUnix()) {
                        sh """
                            mvn test \\
                                -Dsurefire.suiteXmlFiles=${suite} \\
                                ${browserFlag} \\
                                ${aiKeyFlag} \\
                                -Dheadless=${HEADLESS} \\
                                -Dbase.url=${APP_URL} \\
                                -Dapi.base.url=${API_URL} \\
                                -Dmaven.test.failure.ignore=true
                            # WHY maven.test.failure.ignore=true:
                            # Without this flag, Maven exits with code 1 on test failure,
                            # and Jenkins marks the entire pipeline as FAILED before we
                            # can collect artifacts and publish reports. We set it to true
                            # so reporting stages always run, then evaluate pass/fail there.
                        """
                    } else {
                        bat """
                            mvn test -Dsurefire.suiteXmlFiles=${suite} ${browserFlag} ${aiKeyFlag} -Dheadless=${HEADLESS} -Dbase.url=${APP_URL} -Dapi.base.url=${API_URL} -Dmaven.test.failure.ignore=true
                            REM WHY maven.test.failure.ignore=true:
                            REM Without this flag, Maven exits with code 1 on test failure,
                            REM and Jenkins marks the entire pipeline as FAILED before we
                            REM can collect artifacts and publish reports. We set it to true
                            REM so reporting stages always run, then evaluate pass/fail there.
                        """
                    }
                }
            }
        }

        // ----------------------------------------------------------
        stage('Generate Allure Report') {
        // PURPOSE: Convert Allure JSON results → rich HTML report.
        // WHY: Allure results (target/allure-results) are raw JSON — not
        // human-readable. 'mvn allure:report' renders them into an HTML
        // dashboard with graphs, trends, and screenshots.
        // ----------------------------------------------------------
            steps {
                echo "========== STAGE: Generate Allure Report =========="
                script {
                    if (isUnix()) {
                        sh 'mvn allure:report -q'
                    } else {
                        bat 'mvn allure:report -q'
                    }
                }
            }
            post {
                always {
                    // WHY allure([]) PLUGIN:
                    // The Jenkins Allure plugin publishes the HTML report to Jenkins'
                    // build page and maintains a history graph across builds.
                    // Install: Manage Jenkins → Plugins → "Allure Jenkins Plugin"
                    allure([
                        includeProperties: true,
                        jdk: '',
                        properties: [],
                        reportBuildPolicy: 'ALWAYS',
                        results: [[path: 'target/allure-results']]
                    ])
                }
            }
        }

        // ----------------------------------------------------------
        stage('Collect Artifacts') {
        // PURPOSE: Archive test evidence so it's accessible from Jenkins build page.
        // WHY ARTIFACTS MATTER (explicitly required by capstone):
        // - Screenshots: Evidence for failed test cases in defect reports
        // - Allure HTML: Standalone report viewable without Allure server
        // - ExtentReports HTML: Stakeholder-friendly report
        // - Surefire XML: Machine-readable results for downstream tools
        // - Console logs: Debugging CI failures
        // ----------------------------------------------------------
            steps {
                echo "========== STAGE: Collect Artifacts =========="

                // Archive all evidence
                archiveArtifacts artifacts: [
                    'reports/screenshots/**/*.png',
                    'reports/ExtentReport_*.html',
                    'target/allure-report/**',
                    'target/surefire-reports/**',
                    'target/*.log'
                ].join(', '),
                allowEmptyArchive: true,
                fingerprint: true

                // Publish HTML reports (requires HTML Publisher plugin)
                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'allure-report',
                    reportFiles: 'index.html',
                    reportName: 'Allure Report',
                    reportTitles: "Allure Report - Build ${env.BUILD_NUMBER}"
                ])

                publishHTML([
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'reports',
                    reportFiles: "ExtentReport_*.html",
                    reportName: 'Extent Report',
                    reportTitles: "Extent Report - Build ${env.BUILD_NUMBER}"
                ])
            }
        }

        // ----------------------------------------------------------
        stage('Test Results Summary') {
        // PURPOSE: Parse TestNG XML results and display pass/fail counts.
        // WHY: Even with maven.test.failure.ignore=true, we want Jenkins
        // to mark the build UNSTABLE (yellow) if tests failed, vs. SUCCESS
        // (green). This gives the right signal without blocking the pipeline.
        // ----------------------------------------------------------
            steps {
                echo "========== STAGE: Test Results Summary =========="

                // JUnit/TestNG XML publisher — shows pass/fail graph in Jenkins
                junit testResults: 'target/surefire-reports/**/*.xml',
                      allowEmptyResults: true

                script {
                    // Read surefire summary if available
                    def surefireDir = 'target/surefire-reports'
                    if (isUnix()) {
                        sh """
                            echo "========================================="
                            echo "  TEST EXECUTION SUMMARY"
                            echo "  Build Number : ${env.BUILD_NUMBER}"
                            echo "  Branch       : ${env.GIT_BRANCH}"
                            echo "  Timestamp    : \$(date)"
                            echo "========================================="
                            if [ -d ${surefireDir} ]; then
                                echo "Surefire reports found:"
                                ls ${surefireDir}/*.xml 2>/dev/null | wc -l | xargs echo "  XML files:"
                            fi
                        """
                    } else {
                        def surefireDirWin = surefireDir.replace("/", "\\")
                        bat """
                            @echo off
                            echo =========================================
                            echo   TEST EXECUTION SUMMARY
                            echo   Build Number : %BUILD_NUMBER%
                            echo   Branch       : %GIT_BRANCH%
                            echo   Timestamp    : %DATE% %TIME%
                            echo =========================================
                            if exist "${surefireDirWin}" (
                                echo Surefire reports found:
                                (dir /b "${surefireDirWin}\\*.xml" 2>nul | find /c /v "") || cd .
                            )
                        """
                    }
                }
            }
        }
    }

    // ===== POST-PIPELINE ACTIONS =====
    // These run regardless of pipeline result
    post {

        always {
            echo "Pipeline completed. Build: ${env.BUILD_NUMBER}"
            // Clean workspace to free disk space on Jenkins agent
            // WHY: Browser screenshots can be large. Clean up after archiving.
            cleanWs(cleanWhenAborted: true,
                    cleanWhenFailure: false,   // Keep files on failure for debugging
                    cleanWhenSuccess: true)
        }

        success {
            echo "ALL TESTS PASSED — Build ${env.BUILD_NUMBER} is GREEN"
            // In a team project, add email/Slack notification here:
            // slackSend channel: '#qa-alerts', message: "✅ Build ${env.BUILD_NUMBER} passed"
        }

        unstable {
            echo "SOME TESTS FAILED — Build ${env.BUILD_NUMBER} is UNSTABLE (yellow)"
            // slackSend channel: '#qa-alerts', message: "⚠️ Build ${env.BUILD_NUMBER} has test failures"
        }

        failure {
            echo "PIPELINE FAILED — Build ${env.BUILD_NUMBER} — check console logs"
            // slackSend channel: '#qa-alerts', message: "❌ Build ${env.BUILD_NUMBER} pipeline failed"
        }
    }
}