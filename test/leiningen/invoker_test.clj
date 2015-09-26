(ns leiningen.invoker-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [leiningen.invoker :refer :all]
            [clojure.string :as s]))

(defn fixture [file] (io/file "test" "fixtures" file))

(deftest read-simple-invoke-file
  (is (not (= "simple" (read-invoker-file {} (fixture "simple")))))
  (is (= [[:lein "stage"] [:exists? "target/myapp.jar"]] (read-invoker-file {} (fixture "simple")))))
