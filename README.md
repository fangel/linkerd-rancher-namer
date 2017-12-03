# Rancher Namer for Linkerd

This is a **VERY EXPERIMENTAL** and currently **SEMI FUNCTIONAL** namer for
[Linkerd](https://linkerd.io) for use with [Rancher](http://rancher.com).

To run the experiments, first compile the .jar-file:

```bash
./sbt rancher/assembly
```

If you want to run the experiments in Rancher, build a docker-file using
`docker build .`. You probably want to modify the Dockerfile to bake in the
config file. Then push the image somewhere accessible by Rancher, and start up
a service with the following `docker-` and `rancher-compose.yml`-files:

**docker-compose.yml**
```yaml
version: '2'
services:
  helloworld:
    image: scottsbaldwin/docker-hello-world
    labels:
      io.rancher.container.pull_image: always
  linkerd:
    image: [your-image-here]
    ports:
    - 9990
    - 4140
    command:
    - /io/buoyant/linkerd/config.yml
    - -log.level=DEBUG
```

**rancher-compose.yml**
```yaml
version: '2'
services:
  helloworld:
    scale: 2
    start_on_create: true
  linkerd:
    scale: 1
    start_on_create: true
```

You can then send requests to the hello-world service using
`curl -H "Host: helloworld.your-stack-name" http://[rancher-host]:[public-port-for-4140]`

If you scale the helloworld-service up or down, you should be able to see in the
Linkerd admin-UI that the number of endpoints for the client goes up or down
accordingly (with a 15 second delay).

---

Alternatively, you can also use a static mock version of the Rancher Metadata
API which allows you to run the experiments using Docker Compose.
Since the mock metadata-server doesn't support a long-polling functionality, you
probably want to modify `RancherClient.scala` to include a wait after each
request (there is a commented out line that does this). After doing this change
rebuild the plugin, and then start the images using

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

In the Dtab-configuration, we have also created an alias
`sample-service.sample-stack` that load-balances between the to mocked services.
