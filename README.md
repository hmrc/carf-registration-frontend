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
sbt clean compile scalafmtAll coverage test it:test coverageReport 
```

NINOs to use for testing RegistrationService.getIndividualByNino [eg at RegisterDateOfBirthPage]:
```
Example NINOs to generate the 4 possible responses in RegistrationService.getIndividualByNino(...)
JX123456D   OK, Non-empty valid IndividualDetails
WX123456D   OK, Empty IndividualDetails
XX123456D 	NOT FOUND [404]	
YX123456D 	InternalServerError	 [500]

The backend stub "carf-stubs/app/uk/gov/hmrc/carfstubs/helpers/RegistrationHelper" returns the following responses, based upon the first character of the NINO: 
      case "9" | "Y" => InternalServerError("Unexpected error")
      case "8" | "X" => NotFound("Individual user could not be matched")
      case "7" | "W" => Ok(Json.toJson(createEmptyIndividualResponse(request)))
      case _         => Ok(Json.toJson(createFullIndividualResponse(request)))
    
The Full NINO validation regex used by "carf-registration-frontend/app/config/Constants" is:
ninoRegex      =
  "^( [ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]
          |B[A-CEHJ-NPR-TW-Z]
          |G[ACEGHJ-NPR-TW-Z]
          |[KT][A-CEGHJ-MPR-TW-Z]
          |N[A-CEGHJL-NPR-SW-Z]
          |Z[A-CEGHJ-NPR-TW-Y]
   )
   [0-9]{6}
   [A-D ]
   $"
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").