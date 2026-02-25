# carf-registration-frontend

This is the Frontend repository for the Crypto Asset Reporting Framework (CARF) team's registration journey

## What this service does
This service handles organisation and individual registration journeys with integrated navigation, form validation, and HMRC authentication.

### Running the service locally

Prerequisites:
- Java 21
- SBT
- MongoDB
- Service Manager
- Node Version manager (nvm)
- NodeJs

Commands:

Start CARF services in service manager. (frontend,backend, any other services needed to run locally)

```
sm2 --start CARF_ALL
```
Stop this service from service manager.

```
sm2 --stop CARF_REGISTRATION_FRONTEND 
```
Run CARF_REGISTRATION_FRONTEND locally using sbt to test dev changes.

```
sbt run
```

### Running the service in test only mode
```
sm2 --start CARF_ALL
```
```
sm2 --stop CARF_REGISTRATION_FRONTEND
```
Starts service locally with test-only routes enabled.
```
sbt "run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes"
```

### Service manager and port info

Service manager: CARF_ALL

Port: 17000

### How to sign in locally and on staging

Local: 
http://localhost:9949/auth-login-stub/gg-sign-in?continue=http://localhost:17000/register-for-cryptoasset-reporting

Staging:
https://www.staging.tax.service.gov.uk/register-for-cryptoasset-reporting

### Auth wizard setups
Organisation with UTR:
```
Redirect URL: http://localhost:17000/register-for-cryptoasset-reporting
Credential Strength: Strong
Confidence Level: 50
Affinity Group: Organisation
Credential role: User
Enrolments Presets: CT (click Add preset and add Idenitfier value: 1234568945)
```

Organisation without UTR:
```
Affinity Group: Organisation
Credential role: User
Enrolments: leave empty
```

Individual:
```
Affinity Group: Individual
```

Agent: (not valid scenario so will send user to error page)
```
Affinity Group: Agent
```

Assistant: (not valid scenario so will send user to error page)
```
Affinity Group: Organisation
Credential role: Assistant
```

### Running tests
Run unit tests:
```
sbt test
```
Run Integration Tests:
```
sbt it:test
```
Run Unit and Integration Tests with coverage report:
```
sbt clean compile scalafmtAll coverage test it/test coverageReport 
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").