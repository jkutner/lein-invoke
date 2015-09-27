(ns leiningen.invoke
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
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
    (io/file tmpdir (fs/name dir))))

(defn apply-step-exec
  [args dir out]
  (println " > :exec" args)
  (spit out
   (:out (apply shell/sh (flatten (cons args [:dir dir]))))
   :append true)
  success)

; this just calls out to exec but could be smarted in the future
(defn apply-step-lein
  [args dir out]
  (apply-step-exec (cons "lein" args) dir out))

(defn apply-step-exists?
  [args dir]
  (reduce-results
   (map
    (fn [f]
      (if (fs/exists? (io/file dir f))
        success
        (failure (str "file " f " does not exist!"))))
    args)))

(defn apply-step-get
  [args out]
  (println " > :get " args)
  (let [resp (:out (apply shell/sh (cons "curl" args)))]
    (spit out resp)
    resp))

(defn apply-step
  [step dir out]
  (let [step-name (first step)
        step-args (rest step)]
    (case step-name
      :lein (apply-step-lein step-args dir out)
      :exec (apply-step-exec step-args dir out)
      :eval (do (eval (first step-args)) success)
      :exists? (apply-step-exists? step-args dir)
      :contains? (let [value (apply-step (first (rest step-args)) dir out)]
                   (if (.contains value (first step-args))
                       success
                       (do
                        (spit out (str value "\n") :append true)
                        (failure (str "step does not contain " (first step-args))))))
      :get (apply-step-get step-args out)
      :slurp (slurp (first step-args))
      :before success
      :after success)))

(defn invoke-cond-steps
  [key steps dir out]
  (doseq [step steps]
    (if (= key (first step))
      (doseq [sub-step (rest step)]
        (apply-step sub-step dir out)))))

(defn invoke-steps
  "Execute the given invoker steps"
  [steps dir out]
  (try
    (invoke-cond-steps :before steps dir out)
    (reduce-results
     (map
      (fn [step]
        (apply-step step dir out))
      steps))
    (finally
      (invoke-cond-steps :after steps dir out))))

(defn read-invoker-file
  "Read the steps in the invoker file"
  [project dir]
  (eval (load-file (str dir "/invoke.clj"))))

(defn invoke-dir
  "Invoke lein tasks for the project in the given directory"
  [project dir]
  (let [tmpdir (copy-to-temp-dir dir)
        target-dir (str "target/invoker/" (fs/name dir))
        out-file (str target-dir "/invoke.log")]
    (println "Running" (fs/name dir) "...")
    (fs/mkdirs target-dir)
    (spit out-file "")
    (let [result (invoke-steps (read-invoker-file project tmpdir) tmpdir out-file)]
      (println " >" result)
      result)))

(defn invoke-dirs
  "Invoke Lein Task on all projects in the given directory"
  [project dir]
  (println "Invoking tasks on all projects in" dir)
  ;(doseq [f (.listFiles (io/file dir))]
  (reduce-results
    (map (fn [f]
      (if (.isDirectory f)
        (if (fs/exists? (io/file f "invoke.clj"))
          (invoke-dir project f)
          (println "Skipping" (fs/name f) "(no invoke.clj found)"))))
      (.listFiles (io/file dir)))))

(defn invoke
  "Invoke a Lein Task on a project or set of projects"
  [project & cmd]
  (if (not (= success (invoke-dirs project "it"))) (System/exit 1)))
