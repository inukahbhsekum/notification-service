(ns utils.function-utils)

(defn improper-thrush
  [zmap & fns]
  (loop [result zmap
         rem-fns fns]
    (if (seq rem-fns)
      (recur ((first rem-fns) result) (rest rem-fns))
      result)))
