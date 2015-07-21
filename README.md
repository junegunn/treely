# treely

A simple Clojure library for generating tree diagram of nested data structure.

## Leiningen

[![Clojars Project](http://clojars.org/treely/latest-version.svg)](http://clojars.org/treely)

## Usage

### `tree`

    user=> (use 'treely.core)

    user=> (println
      #_=>   (tree [1 [2 3]
      #_=>          4 [5 6]
      #_=>          7 [8 [9 [10]] 11 12]]))
    ├── 1
    │   ├── 2
    │   └── 3
    ├── 4
    │   ├── 5
    │   └── 6
    └── 7
        ├── 8
        │   └── 9
        │       └── 10
        ├── 11
        └── 12

    user=> (println
      #_=>   (tree
      #_=>     '(for [a (range 10)
      #_=>            b (range 10)]
      #_=>        (println (* a b)))
      #_=>     treely.style/ascii)) ; using predefined ascii style
    `-- for
        +-- a
        |   +-- range
        |   `-- 10
        +-- b
        |   +-- range
        |   `-- 10
        `-- println
            +-- *
            +-- a
            `-- b

### `lazy-tree`

    user=> (doseq [str (take 5 (lazy-tree [1 [2 3]
      #_=>                                 4 [5 6]
      #_=>                                 7 [8 [9 [10]] 11 12]]))]
      #_=>   (println str))
    ├── 1
    │   ├── 2
    │   └── 3
    ├── 4
    │   ├── 5

### `dir-tree` / `lazy-dir-tree`

The optional map to `dir-tree` or `lazy-dir-tree` can additionally have
`:filter` function which determines whether a java.io.File instance should be
included or not. Circular symlinks are not handled by default.

```clojure
;;; A poor man's tree
(defn list-files [path]
  (doseq [line
          (letfn [(name [^java.io.File f] (.getName f))
                  (dir? [^java.io.File f] (.isDirectory f))
                  (blue [s] (str \u001b "[34;1m" s \u001b "[m"))]
            (lazy-dir-tree path
              {:filter    #(not= ".git" (name %))
               :formatter #((if (dir? %) blue identity) (name %))}))]
    (println line)))
(list-files ".")
```

### Options

#### Keys

| Key            | Type     | Default  |
| ---            | ---      | ---      |
| `:indent`      | String   | `"　　"` |
| `:bar`         | String   | `"│ 　"` |
| `:branch`      | String   | `"├── "` |
| `:last-branch` | String   | `"└── "` |
| `:formatter`   | Function | `str`    |

#### Predifined styles (`treely.style`)

| Key            | unicode  | unicode-slim | ascii      |
| ---            | ---      | ---          | ---        |
| `:indent`      | `"　　"` | `"　"`       | ``"    "`` |
| `:bar`         | `"│ 　"` | `"│ "`       | ``"|   "`` |
| `:branch`      | `"├── "` | `"├ "`       | ``"+-- "`` |
| `:last-branch` | `"└── "` | `"└ "`       | ``"`-- "`` |

## License

MIT
