# Rancher Namer for Linkerd

This is a **VERY EXPERIMENTAL** and currently **SEMI FUNCTIONAL** namer for
[Linkerd](https://linkerd.io) for use with [Rancher](http://rancher.com).

To run the experiment, first compile the .jar-file:

```bash
./sbt rancher/assembly
```

Next you can start up the Docker-containers using Docker Compose:

```bash
docker-compose up --build
```

The demo starts up 3 hello-world containers named `sample-service1` through
`..3`. These are mapped where `1` and `3` belong to the service
`sample-stack/sample-service1` and `2` belongs to the service
`sample-stack/sample-service2`.

```bash
curl -H "Host: sample-service1.sample-stack" http://127.0.0.1:4140
```
(If your Docker-host is your local machine - otherwise substitute in the ip of
your Docker host)

In the Dtab, we have also created an alias `sample-service.sample-stack` that
load-balances between the to mocked services.

You should now be able to see in the output from Linkerd that we call the (mock)
metadata-API every 15 seconds.
