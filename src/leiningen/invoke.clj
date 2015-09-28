(ns leiningen.invoke
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.java.shell :as shell]
            [me.raynes.fs :as fs]
            [cemerick.pomegranate.aether :as aether]
            [leiningen.core.project :as project]
            [leiningen.core.classpath :as classpath]
            [leiningen.core.main :as main]))

(defn create-result
  [success step message]
  {:success success, :step step, :message message})

(def workdir "target/invoker")

(defn success [& args] (create-result true (first args) (second args)))

(defn failure [step msg] (create-result false step msg))

(defn reduce-results
  [results]
  (let [failures (remove (fn [r] (:success r)) results)]
    (if (empty? failures) [(success :all)] failures)))

(defn gsub
  [s re v]
  (str/replace s (re-pattern re) v))

(defn copy-to-temp-dir
  [project dir]
  (fs/mkdirs workdir)
  (fs/copy-dir dir workdir)
  (let [newdir (io/file workdir (fs/name dir))
        project-clj (io/file newdir "project.clj")]
    (if (fs/exists? project-clj)
      (spit project-clj (gsub (slurp project-clj) "@project_version@" (:version project))))
    newdir))

(defn apply-step-exec
  [args dir out]
  (let [msg (apply shell/sh (flatten (cons args [:dir dir])))]
    (spit out (:out msg) :append true)
    (spit out (:err msg) :append true)
    (success :exec (:out msg))))

; this just calls out to exec but could be smarted in the future
(defn apply-step-lein
  [args dir out]
  (apply-step-exec (cons "lein" args) dir out))

(defn apply-step-exists?
  [args dir]
  (let [f (first args)]
    (if (fs/exists? (io/file dir f))
      (success :exists?)
      (failure :exists? (str f " does not exist!")))))

(defn apply-step-get
  [args out]
  (let [resp (:out (apply shell/sh (cons "curl" args)))]
    (spit out resp :append true)
    (success :get resp)))

(defn apply-step
  [step dir out]
  (let [step-name (first step)
        step-args (rest step)]
    (println ">" step-name step-args)
    (case step-name
      :lein (apply-step-lein step-args dir out)
      :exec (apply-step-exec step-args dir out)
      :eval (do (eval (first step-args)) (success :eval))
      :exists? (apply-step-exists? step-args dir)
      :contains? (let [value (apply-step (first (rest step-args)) dir out)]
                   (if (.contains (:message value) (first step-args))
                       (success :contains?)
                       (do
                         (spit out (str value "\n") :append true)
                         (failure :contains? (str "step does not contain: " (first step-args))))))
      :get (apply-step-get step-args out)
      :slurp (success :slurp (slurp (str (first step-args))))
      :before (success :before)
      :after (success :after))))

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
  (let [tmpdir (copy-to-temp-dir project dir)
        out-file (str tmpdir "/invoke.log")]
    (println "Running" (fs/name dir) "...")
    (spit out-file "")
    (let [results (invoke-steps (read-invoker-file project tmpdir) tmpdir out-file)]
      (if (not (:success (first results))) (println (slurp out-file)))
      (doseq [result results]
        (println ">"
                 (if (:success result)
                   "SUCCESS"
                   (str "FAILURE: " (:message result)))))
      (first results))))

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
          (do (println "Skipping" (fs/name f) "(no invoke.clj found)") (success :skip)))
        (success :skip)))
      (.listFiles (io/file dir)))))

(defn invoke
  "Invoke a Lein Task on a project or set of projects"
  [project & cmd]
  (fs/delete-dir workdir)
  (if (:success
             (if (empty? cmd)
               (first (invoke-dirs project "it"))
               (invoke-dir project (first cmd))))
      (println "Invoker SUCCESS")
      (do (main/warn "Invoker FAILURE!") (System/exit 1))))
