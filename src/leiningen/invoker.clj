(ns leiningen.invoker
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [cemerick.pomegranate.aether :as aether]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]))

(defn copy-to-temp-dir
  [dir]
  ; create temp dir
  ; cpy dir into tmp dir
  )

(defn invoke-steps
  "Execute the given invoker steps"
  [project dir steps]
  (doseq [step steps]
    (print step)
    ))

(defn read-invoker-file
  "Read the steps in the invoker file"
  [project dir]
  (eval (load-file (str dir "/invoke.clj"))))

(defn invoke-dir
  "Invoke a Lein Task on a project"
  [project dir]
  (doseq [f (.listFiles (io/file dir))]
    (if (.isDirectory f)
      (print "d ")
      (print "- "))
    (println (.getName f))))

(defn invoke
  "Invoke a Lein Task on a project or set of projects"
  [project & cmd]
  (apply invoke-dir "it"))
