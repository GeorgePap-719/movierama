# Movierama

A social sharing platform where users can share their favorite movies.

## Build environment requirements

- JDK >= 17
- Mysql 8.x
- npm 10.5.0

## Building

This project is split into two modules FE and BE built with npm and Gradle respectively.

- Run `./gradlew assemble` to build the project and produce the .jar executable.
- Run `./gradlew test` to run all the tests (Mysql instance running required).

You can import this project into IDEA, but you have to delegate build actions to Gradle (in
Preferences -> Build, Execution, Deployment -> Build Tools -> Gradle -> Build and run).

After producing the required artifacts to run the back-end server
use `java -jar build/libs/workable-assignment-0.0.1-SNAPSHOT.jar`.
Note that the server connects with a Mysql server.
Use the [sql init script](src/main/resources/db/initdb.sql) to create the
database's schema.
In case you want to connect to another Mysql instance beside localhost or change the connection's
properties, you can change them
in [application.properties](src/main/resources/application.properties) accordingly.

To run the frontend run `npm start`, it will run on `localhost:3000`.
Note that to run this command, you have to be inside the `frontend` directory.