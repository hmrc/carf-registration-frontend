# carf-registration-frontend

This is the Frontend repository for the Crypto Asset Reporting Framework (CARF) team's registration journey

## What this service does
- Organisation and individual registration for CARF reporting
- Agent/assistant journey handling
- UTR and non-UTR registration flows
- Integrates with HMRC authentication and authorisation services

### Running the service locally

Prerequisites:
- Java 11
- SBT
- MongoDB
- Service Manager

Commands:
```
sm2 --start CARF_REGISTRATION_ALL
```
```
sm2 --stop CARF_REGISTRATION_FRONTEND 
```
```
sbt run
```

### Running the service in test only mode
```
sm2 --start CARF_REGISTRATION_ALL
```
```
sm2 --stop CARF_REGISTRATION_FRONTEND
```
```
sbt "run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes"
```

### Service manager and port info

Service manager: CARF_ALL

Port: 17000

### How to sign in locally and on staging

Local: 
http://localhost:9949/auth-login-stub/gg-sign-in?continue=http://localhost:17000/register-for-carf

Staging:
https://www.staging.tax.service.gov.uk/register-for-carf

### Auth wizard setups
Organisation with UTR:
```
Redirect URL: http://localhost:17000/register-for-carf
Credential Strength: Strong
Confidence Level: 50
Affinity Group: Organisation
Enrolments Presets: CT (click Add preset and add Idenitfier value: 1234568945)
```

Organisation without UTR:
```
Affinity Group: Organisation
Enrolments: leave empty
```

Individual with UTR:
```
Affinity Group: Individual
Enrolments Presets: CT (click Add preset and add Idenitfier value: 1234568945)
```


Individual without UTR:
```
Affinity Group: Individual
Enrolments: leave empty
```

Agent/Assistant:
```
Affinity Group: Agent
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
sbt clean compile coverage test it:test coverageReport
```
### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").