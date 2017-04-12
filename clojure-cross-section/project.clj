(defproject clojure-cross-section "0.0.1-SNAPSHOT"
  :description "r/place activity cross section service"
  :url "https://github.com/srudolph-credera/opentracingdemo/clojure-cross-section"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [io.pedestal/pedestal.service "0.5.2"]
                 [io.pedestal/pedestal.jetty "0.5.2"]
                 [ch.qos.logback/logback-classic "1.1.8" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.22"]
                 [org.slf4j/jcl-over-slf4j "1.7.22"]
                 [org.slf4j/log4j-over-slf4j "1.7.22"]
                 [io.opentracing.brave/brave-opentracing "0.19.2"]
                 [io.zipkin.reporter/zipkin-sender-okhttp3 "0.6.13"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  ;; If you use HTTP/2 or ALPN, use the java-agent to pull in the correct alpn-boot dependency
  ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.5"]]
  :profiles {:dev {:aliases {"run-dev" ["trampoline" "run" "-m" "clojure-cross-section.server/run-dev"]}
                   :dependencies [[io.pedestal/pedestal.service-tools "0.5.2"]]}
             :uberjar {:aot [clojure-cross-section.server]}}
  :main ^{:skip-aot true} clojure-cross-section.server)

