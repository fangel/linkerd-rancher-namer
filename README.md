# Rancher Namer for Linkerd

This is a **VERY EXPERIMENTAL** and currently **NOT FUNCTIONAL** namer for
[Linkerd](https://linkerd.io) for use with [Rancher](http://rancher.com).

To run the experiment, first compile the .jar-file:

```bash
./sbt rancher/assembly
```

Next you can start up the Docker-containers using Docker Compose:

```bash
docker-compose up --build
```

You can then give our sample service a request by issuing

```bash
curl -H "Host: sample-service" http://127.0.0.1:4140
```
(If your Docker-host is your local machine - otherwise substitute in the ip of
your Docker host)

You should now be able to see in the output from Linkerd that we call the (mock)
metadata-API every 15 seconds, and it outputs back the `Address` of the instance
of our service.

---

To simulate that our sample service changes ip to a different provider, you
can then open and edit `mock-metadata-api/containers` and modify the
`primary_ip` field to be `sample-service2` instead of `sample-service1`  
Yes, I've cheated a bit in my mock Rancher metadata-API and specified some
hostnames instead of ips, because it made it simpler to test. But disregard that.

After a refresh interval, you will see the plugin output the `Address` of the
other service. But if you give it another request with the above mentioned
cURL-command, you will see that the first host is still the one serving the
response (indicated by both the access-log coming from the first sample-service
and the fact that the hostname that is included in the response is identical to
the one from before the service-name was changed)
