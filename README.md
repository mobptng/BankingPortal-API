# Banking Portal API fork

This is a fork of the repository [abhi9720/BankingPortal-API](https://github.com/abhi9720/BankingPortal-API?tab=MIT-1-ov-file#readme)

## Add-ons

### docker-compose

Ensure Docker Compose is installed on your machine by running the command below. If not, download and install
from [Docker's official site](https://www.docker.com/get-started).

   ```bash
   docker compose
   ```
If docker compose is installed, you'll see a list of docker compose commands. If not, you'll receive a "command not found" error.
Alternatives like podman will do the trick as well. 

### MySQL

Before running the project, we need a MySQL server with a database named `bankingapp`.
To start it, run the docker-compose file from the `database` folder

    ```bash
    docker compose up
    ```

### Jacoco

Jacoco is a code coverage tool. We will use it to generate a report of the code coverage of the tests.
To run the tests and generate a report run the following command:

```bash
mvn clean test jacoco:report
```

The report will be generated in the `target/site/jacoco` folder.

### PIT 

[PIT](https://pitest.org) is a tool to perform mutation testing. It can be used to generate a report with

```bash
mvn clean org.pitest:pitest-maven:mutationCoverage
```

## Running the application

1. Start up the database (see previous section)
2. Create a copy of the `application.properties.sample` file called `application.properties`. Adjust the 
`datasource.password` property to the value in the database setup. The mail section can be left unchanged (and
exceptions due to it ignored).
3. Run `mvn spring-boot:run`. The application should launch in a few seconds.