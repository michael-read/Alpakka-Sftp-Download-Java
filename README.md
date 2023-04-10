# Alpakka Sftp Download Example

This is a simple Akka Java 11 application that leverages Akka Streams with the [Alpakka FTP](https://doc.akka.io/docs/alpakka/current/ftp.html) connector. 

The application takes its configuration from `application.conf` and then transfers files from the source directory to the destination directory.

Build is accomplished through the `sbt`.

1. Update the `application.conf` file contained in src/main/resources:
```
app {
  host = "192.168.122.50"
  port = 22
  username = "akka"
  password = "akka"

  source-dir = "/home/akka/files"
  target-dir = "./target"
}
```
2. Run the application from the project's root directory:
```
$ sbt run
```

The implementation of the file transfer is handled by the `SftpDownloadActor`.