
def twitterUtil(mod: String) =
  "com.twitter" %% s"util-$mod" %  "6.45.0"

def finagle(mod: String) =
  "com.twitter" %% s"finagle-$mod" % "6.45.0"

def linkerd(mod: String) =
  "io.buoyant" %% s"linkerd-$mod" % "1.2.0"

val jacksonVersion = "2.8.4"

val rancher =
  project.in(file("rancher")).
    settings(
      scalaVersion := "2.12.1",
      organization := "io.buoyant",
      name := "rancher",
      resolvers ++= Seq(
        "twitter" at "https://maven.twttr.com",
        "local-m2" at ("file:" + Path.userHome.absolutePath + "/.m2/repository")
      ),
      libraryDependencies ++=
        finagle("http") % "provided" ::
        twitterUtil("core") % "provided" ::
        linkerd("core") % "provided" ::
        linkerd("protocol-http") % "provided" ::
        "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion % "provided" ::
        "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion % "provided" ::
        "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion % "provided" ::
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion % "provided" ::
        Nil,
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
    )
