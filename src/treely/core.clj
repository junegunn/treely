(ns treely.core
  "Functions to generate tree diagram of nested data structure"
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [treely.style]))

;;; For a cleanly laid-out data structure, it is straightforward to obtain the
;;; tree representation of it using a simple recursive algorithm.
;;;
;;;   => (println (tree [1 2 [3 [4 5] 6]]))
;;;
;;;     ├── 1
;;;     └── 2
;;;         ├── 3
;;;         │   ├── 4
;;;         │   └── 5
;;;         └── 6
;;;
;;; However, if there are contiguous sub-lists of the same level, a naive
;;; method will generate broken tree diagram.
;;;
;;;   => (println (tree [1 2 [3 [4] [5] 6]]))
;;;
;;;     ├── 1
;;;     └── 2
;;;         ├── 3
;;;         │   └── 4
;;;         │   └── 5
;;;         └── 6
;;;
;;; Such cases are often found when dealing with s-expressions.
;;;
;;;   => (println
;;;   =>   (tree
;;;   =>     '(for [a (range 10)
;;;   =>            b (range 10)]
;;;   =>        (println (* a b)))))
;;;
;;;     └── for
;;;     　　├── a
;;;     　　│ 　├── range
;;;     　　│ 　└── 10
;;;     　　└── b
;;;     　　　　├── range
;;;     　　　　└── 10
;;;     　　└── println
;;;     　　　　├── *
;;;     　　　　├── a
;;;     　　　　└── b
;;;
;;; So in order not to yield broken tree diagram, we have to recursively merge
;;; contiguous lists of the same level. But I was unable to find
;;; a non-stack-consuming way of doing it, and I ended up transforming the
;;; algorithm into an iterative version with lazy sequences which yielded much
;;; better performance and avoided stack overflows. However, determining the
;;; shape of the branch with the iterative algorithm inherently requires
;;; unbounded lookahead which can be problematic for very large lists, so some
;;; tricks are used to minimize unnecessary lookaheads.

(defn- options-for
  "Returns the default options extended by the given map"
  [options]
  (merge {:formatter str} treely.style/unicode options))

(defn- container?
  "Returns if the given argument is a container for child elements"
  [e]
  (and (or (vector? e) (seq? e))))

(defn- ^String elem->str
  "Returns the line for the element in the tree diagram. Can handle multi-line
  elements."
  [elem indent last? options]
  (let [lines (s/split-lines ((:formatter options) elem))
        ;; If the string representation of the element only contains new line
        ;; characters, it will be presented as an empty string
        lines (or (seq lines) [""])]
    (s/join "\n" (map-indexed #(str indent
                                    ((if last?
                                       (if (zero? %) :last-branch :indent)
                                       (if (zero? %) :branch :bar)) options)
                                    %2) lines))))

(defn- find-last-hint
  "Generates hints to avoid unnecessary lookahead when determining if a node is
  the last branch in the subtree it belongs to. This function is non-lazy and
  goes over the entire collection, so you may notice a hiccup if the subtree is
  considerably large. But the iteration stays at the top level and does not
  descend into the subtrees, so it is still more responsive than the
  alternative approaches."
  [coll]
  (loop [last-non-container -1
         container-ends #{}
         coll (map-indexed vector coll)]
    (let [[[idx1 head]
           [idx2 sec] & tail] coll
          head-cont? (container? head)
          sec-cont?  (container? sec)
          head-elem? (and head (not head-cont?))
          sec-elem?  (and sec (not sec-cont?))
          last-non-container (cond sec-elem?  idx2
                                   head-elem? idx1
                                   :else last-non-container)]
      (if head
        (recur last-non-container
               (if (and head-cont? (not sec-cont?))
                 (conj container-ends idx1) container-ends)
               (if sec-elem? tail (rest coll)))
        {:last-non-container last-non-container
         :container-ends     container-ends}))))

(defn- depths
  "Returns the lazy sequence of [element depth can-be-last?] triples for the
  given nested data structure"
  ([coll] (depths coll 0 true))
  ([coll depth can-be-last?]
   (let [{:keys [last-non-container container-ends]} (find-last-hint coll)]
     (mapcat #(if (container? %)
                (depths % (inc depth) (container-ends %2))
                [[% depth (and can-be-last? (= last-non-container %2))]])
             coll
             (iterate inc 0)))))

(defn- follow
  "Iterates over the sequence of triples obtained by depths function and
  returns the lazy sequence of strings"
  [depths options]
  ((fn nxt [depths indents depth was-last?]
     (let [[[head head-depth can-be-last?] & tail] depths
           desc  (take-while #(>= (second %) head-depth) tail)
           last? (and can-be-last?
                      (or (empty? desc)
                          (empty? (filter #(= (second %) head-depth) desc))))]
       (when head
         (let [indents
               (cond (= head-depth depth) indents
                     (> head-depth depth)
                     (conj indents ((if was-last? :indent :bar) options))
                     :else (vec (take head-depth indents)))]
           (cons
             (elem->str head (apply str indents) last? options)
             (lazy-seq (nxt tail indents head-depth last?)))))))
   depths [] 0 false))

(defn lazy-tree
  "Returns the lazy sequence of strings that constitute the tree diagram of the
  given nested data structure. Optionally takes a map that determines the style
  of the output. Valid keys to the map are as follows.

  | Key            | Default    |
  | ---            | ---        |
  | `:indent`      | `\"　　\"` |
  | `:bar`         | `\"│ 　\"` |
  | `:branch`      | `\"├── \"` |
  | `:last-branch` | `\"└── \"` |
  | `:formatter`   | `str`      |"
  [elems & [options]]
  (follow (depths elems) (options-for options)))

(defn ^String tree
  "Returns the tree diagram of the given nested data structure in String. This
  is essentially the same as the following:

      (clojure.string/join \"\\n\" (lazy-tree elems options))"
  [elems & [options]]
  (s/join "\n" (lazy-tree elems options)))

(defn lazy-dir-tree
  "Returns the lazy sequence of strings that constitute the tree diagram of the
  directory contents. The optional map can additionally have `:filter` function
  which determines whether a java.io.File instance should be included or not."
  [path & [options]]
  (let [root (io/file path)
        filt (get options :filter identity)
        opts (options-for (dissoc options :filter))
        fmt  (:formatter opts)
        walk (fn walk [node]
               (when (filt node)
                 (cons node (when (.isDirectory node)
                              (remove nil? (map walk (.listFiles node)))))))]
    (cons
      (fmt root)
      (when (.isDirectory root)
        (let [children (filter filt (.listFiles root))]
          (lazy-tree (mapcat walk children) opts))))))

(defn ^String dir-tree
  "Returns the tree diagram of the directory contents under the given path in
  String"
  [path & [options]]
  (s/join "\n" (lazy-dir-tree path options)))
