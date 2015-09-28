(ns leiningen.invoke-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [leiningen.invoke :refer :all]
            [clojure.string :as s]))

(defn fixture [file] (io/file "test" "fixtures" file))

(deftest read-simple-invoke-file
  (is (not (= "simple" (read-invoker-file {} (fixture "simple")))))
  (is (= [[:lein "stage"] [:exists? "target/myapp.jar"]] (read-invoker-file {} (fixture "simple")))))

(deftest read-stateful-invoke-file
  (is (= [[:lein "foobar"] [:exec "foobar"]] (read-invoker-file {} (fixture "stateful")))))

(deftest reduces-success-result
  (let [results (reduce-results [(success)])]
    (is (:success (first results)))
    (is (= 1 (count results)))))


(deftest reduces-success-results
  (let [results (reduce-results [(success) (success) (success)])]
    (is (= 1 (count results)))
    (is (:success (first results)))))

(deftest reduces-failure-result
  (let [results (reduce-results [(failure :test "it broke")])]
    (is (not (:success (first results))))
    (is (not (:success (last results))))))

(deftest reduces-failure-results
  (let [results (reduce-results [(success) (failure :test "it broke") (success)])]
    (is (not (:success (first results))))
    (is (not (:success (last results))))))

(deftest exists?-postive-test
  (is (:success (apply-step-exists? ["invoke.clj"] (fixture "simple")))))

(deftest exists?-postive-test
  (is (not (:success (apply-step-exists? ["foobar.clj"] (fixture "simple"))))))

(deftest invokes-cond-steps
  (is (thrown? UnsupportedOperationException
        (invoke-cond-steps :x [[:x [:eval '(throw (UnsupportedOperationException. "test"))]]] nil nil))))

(deftest does-not-invoke-cond-steps
  (is (= nil (invoke-cond-steps :x [[:y [:eval '(throw (Exception. "test"))]]] nil nil))))

(deftest invokes-before-steps
  (is (thrown? UnsupportedOperationException
               (invoke-steps [[:before [:eval '(throw (UnsupportedOperationException. "test"))]]] nil nil))))

(deftest invokes-before-steps
  (let [result (invoke-steps [[:exists? "fake-file"] [:exists? "fake-file2"]] nil nil)]
    (is (= 2 (count result)))
    (is (not (:success (first result))))
    (is (not (:success (second result))))))


(deftest invokes-after-steps
  (is (thrown? UnsupportedOperationException
       (invoke-steps
        [[:eval (delay 1)]
         [:after [:eval '(throw (UnsupportedOperationException. "test"))]]]
        nil nil))))

(deftest applies-step-eval
  (is (:success (apply-step [:eval '(+ 1 1)] nil nil))))

(deftest invokes-ls-on-a-dir
  (invoke-dir {} (fixture "output"))
  (is (= "asdfasdf\n" (slurp "target/invoker/output/invoke.log"))))

(deftest succeeds-on-contains?
  (is (:success
         (apply-step
          [:contains? "lein-invoke" [:slurp "project.clj"]]
          nil
          (fs/temp-file "lein-invoke-test")))))

(deftest success-on-get
  (is (:success
         (apply-step
          [:contains? "crocodile" [:get "www.httpbin.org/get?arg=crocodile"]] nil (fs/temp-file "lein-invoke-test")))))
