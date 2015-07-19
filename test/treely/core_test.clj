(ns treely.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [trimr]]
            [treely.core :refer :all]
            [treely.style]))

(def unicode-result (trimr (slurp "test/treely/fixtures/unicode.txt")))
(def ascii-result   (trimr (slurp "test/treely/fixtures/ascii.txt")))
(def sample-tree
  [1 [2 3]
   4 [5] [6]
   7 [8 [9 [10]] "11\nfoo\nbar" 12]
   13 ["14\nfoo\nbar" ["15\nfoo\nbar"]]
   16 [17 [18 ["\n" [20 [21] 22 [23 [24]]]]] 25]])

(deftest tree-test
  (testing "default"
    (is (= unicode-result
           (tree sample-tree))))
  (testing "unicode"
    (is (= unicode-result
           (tree sample-tree treely.style/unicode))))
  (testing "ascii + :formatter"
    (is (= ascii-result
           (tree sample-tree
                 (assoc treely.style/ascii
                        :formatter
                        #(str "(" % ")")))))))
