# Duplicate requests from Jersey Client

This program reproduces a problem that we've seen with Jersey Client, in which it will duplicate some requests.

*N.b. I haven't found the root cause yet, but it seems to happen if:*

1. *We make both PUT and GET requests*
2. *Fail to close the GET requests response*

## Notes

- It is not enough to ignore the PUT response, it seems to require a different endpoint
- It happens occationally with a single threaded client loop – but the ExecutorService makes it much more frequent
- `org/glassfish/jersey/client/HttpUrlConnectorProvider.java:278` does NOT call into `return (HttpURLConnection) url.openConnection()` more than once, so the duplicate seems to happen in HttpURLConnection

*N.b. This will also cause duplicate requests, which suggests that it isn't the Jersey Client that is at fault...*

```
            es.submit(() -> {
                try {
                    URL url = new URL(String.format("%s?id=%s", BASE_URI, id));

                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("PUT");
                    con.setDoOutput(true);
                    InputStream is = con.getInputStream();
                    is.close();
                } catch (IOException e) {
                }
            });
            es.submit(() -> {
                try {
                    URL url = new URL(BASE_URI);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    InputStream is = con.getInputStream();

                    // This prevents the duplicate requests.
                    // is.close();
                } catch (IOException e) {
                }
            });
```


## Example

```
$ mvn compile exec:java
[INFO] Scanning for projects...
[WARNING]
[WARNING] Some problems were encountered while building the effective model for com.folkol:rs-client:jar:1.0-SNAPSHOT
[WARNING] 'build.plugins.plugin.version' for org.apache.maven.plugins:maven-compiler-plugin is missing. @ line 50, column 15
[WARNING]
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.
[WARNING]
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.
[WARNING]
[INFO]
[INFO] ------------------------< com.folkol:rs-client >------------------------
[INFO] Building rs-client 1.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO]
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ rs-client ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /Users/folkol/code/rs-client/src/main/resources
[INFO]
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ rs-client ---
[INFO] Nothing to compile - all classes are up to date
[INFO]
[INFO] >>> exec-maven-plugin:1.2.1:java (default-cli) > validate @ rs-client >>>
[INFO]
[INFO] <<< exec-maven-plugin:1.2.1:java (default-cli) < validate @ rs-client <<<
[INFO]
[INFO]
[INFO] --- exec-maven-plugin:1.2.1:java (default-cli) @ rs-client ---
Sep 01, 2018 12:46:24 PM org.glassfish.grizzly.http.server.NetworkListener start
INFO: Started listener bound to [localhost:8080]
Sep 01, 2018 12:46:24 PM org.glassfish.grizzly.http.server.HttpServer start
INFO: [HttpServer] Started.
Duplicate request: 187
Duplicate request: 425
Duplicate request: 423
Duplicate request: 663
Duplicate request: 661
Duplicate request: 665
Duplicate request: 889
Duplicate request: 890
Duplicate request: 1428
Duplicate request: 1427
Duplicate request: 1660
Duplicate request: 1874
Duplicate request: 2060
Duplicate request: 2228
Duplicate request: 2230
Duplicate request: 2736
Duplicate request: 3402
Duplicate request: 3404
Duplicate request: 3405
Duplicate request: 3406
...
```

## Refrences

- [http://jersey.576304.n2.nabble.com/Is-Jersey-Jersey-client-duplicating-requests-td6570645.html]()
- [https://stackoverflow.com/questions/37956741/jersey-resource-receiving-duplicate-requests-from-jersey-client]()
