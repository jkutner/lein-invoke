(ns leiningen.invoker
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [cemerick.pomegranate.aether :as aether]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main])
  (:import [java.io File]))

(defn- invoke-task
  "Invoke a Lein Task on a project"
  [project dir]
  (doseq [f (.listFiles (new File dir))]
    (if (.isDirectory f)
      (print "d ")
      (print "- "))
    (println (.getName f))))

(defn invoke
  "Invoke a Lein Task on a project or set of projects"
  [project & cmd]
  (apply invoke-task "it"))
