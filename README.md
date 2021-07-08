# Personal Work Log

This is a CLI to simply log my daily works. The implementation uses [Scalar DB](https://github.com/scalar-labs/scalardb) as backend.

# Build

It needs:
- OpenJDK for Java 8.0
- Gradle 6

then the following command can build the CLI executables.

```
./gradlew installDist
```

The executables will be outputed to `app/build/install/app/bin/app`.

# Setup

Scalar DB links to a real distributed database cluster. It supports Cassandra, DynamoDB, ... etc.

We needs to:
- Create a cluster of Cassandra, DynamoDB, or whatelse supported by Scalar DB
- Import the schema [plenty.su](./plenty.json) to the cluster. [Reference](https://github.com/scalar-labs/scalardb/blob/master/docs/getting-started-with-scalardb.md#set-up-database-schema)
- Configure the Scalar DB via [sclardb.properties](./scalardb.properties.cassandra)

to execute the CLI.

# Use

Use `app <log>` to log a work. It can record as many logs as you want.

Use `app` to display today's logs.
