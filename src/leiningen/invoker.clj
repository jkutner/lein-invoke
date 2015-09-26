(ns leiningen.invoker
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [me.raynes.fs :as fs]
            [cemerick.pomegranate.aether :as aether]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]))

(def success "SUCCESS")

(defn failure [msg] (str "FAILURE: " msg))

(defn reduce-results
  [results]
  (reduce
   (fn [previous next]
     (if (= previous next)
       next
       (if (= previous success) next previous)))
   results))

(defn copy-to-temp-dir
  [dir]
  (let [tmpdir (fs/temp-dir "lein-invoker")]
    (fs/copy-dir dir tmpdir)
    tmpdir))

(defn apply-step-exec
  [args dir out]
  (print "EXEC" args)
  (success))

; this just calls out to exec but could be smarted in the future
(defn apply-step-lein
  [args dir out]
  (apply-step-exec (apply str (cons "lein " args)) dir out))

(defn apply-step-exists?
  [args dir]
  (reduce-results
   (map
    (fn [f]
      (if (fs/exists? (io/file dir f))
        success
        (failure (str "file " f " does not exist!"))))
    args)))

(defn apply-step
  [step dir out]
  (let [step-name (first step)
        step-args (rest step)]
    (case step-name
      :lein (apply-step-lein step-args dir out)
      :exec (apply-step-exec step-args dir out)
      :exists? (apply-step-exists? step-args dir))))

(defn invoke-steps
  "Execute the given invoker steps"
  [steps dir out]
  (doseq [step steps] (apply-step step dir out)))

(defn read-invoker-file
  "Read the steps in the invoker file"
  [project dir]
  (eval (load-file (str dir "/invoke.clj"))))

(defn invoke-dir
  "Invoke lein tasks for the project in the given directory"
  [project dir]
  (let [tmpdir (copy-to-temp-dir dir)]
    (fs/mkdirs (io/file "target" (fs/name dir)))
    ;; create output file
    (invoke-steps (read-invoker-file tmpdir) tmpdir nil)))

(defn invoke-dirs
  "Invoke Lein Task on all projects in the given directory"
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
