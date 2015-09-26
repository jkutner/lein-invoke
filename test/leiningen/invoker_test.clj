(ns leiningen.invoker-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [leiningen.invoker :refer :all]
            [clojure.string :as s]))

(defn fixture [file] (io/file "test" "fixtures" file))

(deftest read-simple-invoke-file
  (is (not (= "simple" (read-invoker-file {} (fixture "simple")))))
  (is (= [[:lein "stage"] [:exists? "target/myapp.jar"]] (read-invoker-file {} (fixture "simple")))))

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
