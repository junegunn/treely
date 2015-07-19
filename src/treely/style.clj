(ns treely.style
  "A predefined set of maps for tree styles.

  Each map defines the following attributes:

  - `:indent`
  - `:bar`
  - `:branch`
  - `:last-branch`")

(def unicode
  "Tree style with Unicode characters (default)"
  {:indent      "    "
   :bar         "│   "
   :branch      "├── "
   :last-branch "└── "})

(def unicode-slim
  "Tree style with Unicode characters (slimmer)"
  {:indent      "　"
   :bar         "│ "
   :branch      "├ "
   :last-branch "└ "})

(def ascii
  "Tree style with ascii characters"
  {:indent      "    "
   :bar         "|   "
   :branch      "+-- "
   :last-branch "`-- "})

