FROM buoyantio/linkerd:1.3.1

ADD rancher/target/scala-2.12/rancher-assembly-0.1-SNAPSHOT.jar $L5D_HOME/plugins/rancher.jar
