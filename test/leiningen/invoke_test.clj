(ns leiningen.invoke-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [leiningen.invoke :refer :all]
            [clojure.string :as s]))

(defn fixture [file] (io/file "test" "fixtures" file))

(deftest read-simple-invoke-file
  (is (not (= "simple" (read-invoker-file {} (fixture "simple")))))
  (is (= [[:lein "stage"] [:exists? "target/myapp.jar"]] (read-invoker-file {} (fixture "simple")))))

(deftest read-stateful-invoke-file
  (is (= [[:lein "foobar"] [:exec "foobar"]] (read-invoker-file {} (fixture "stateful")))))

(deftest reduces-success-result
  (is (= success (reduce-results [success]))))

(deftest reduces-success-results
  (is (= success (reduce-results [success success success]))))

(deftest reduces-failure-result
  (is (not (= success (reduce-results [(failure "it broke")])))))

(deftest reduces-failure-results
  (is (not (= success (reduce-results [success (failure "it broke") success])))))

(deftest exists?-postive-test
  (is (= success (apply-step-exists? ["invoke.clj"] (fixture "simple")))))

(deftest exists?-postive-test
  (is (not (= success (apply-step-exists? ["foobar.clj"] (fixture "simple"))))))

(deftest invokes-cond-steps
  (is (thrown? UnsupportedOperationException
        (invoke-cond-steps :x [[:x [:eval '(throw (UnsupportedOperationException. "test"))]]] nil nil))))

(deftest does-not-invoke-cond-steps
  (is (= nil (invoke-cond-steps :x [[:y [:eval '(throw (Exception. "test"))]]] nil nil))))

(deftest invokes-before-steps
  (is (thrown? UnsupportedOperationException
        (invoke-steps [[:before [:eval '(throw (UnsupportedOperationException. "test"))]]] nil nil))))

(deftest invokes-after-steps
  (is (thrown? UnsupportedOperationException
       (invoke-steps
        [[:eval (delay 1)]
         [:after [:eval '(throw (UnsupportedOperationException. "test"))]]]
        nil nil))))

(deftest applies-step-eval
  (is (= success (apply-step [:eval '(+ 1 1)] nil nil))))

(deftest invokes-ls-on-a-dir
  (invoke-dir {} (fixture "output"))
  (is (= "asdfasdf\n" (slurp "target/invoker/output/invoke.log"))))
