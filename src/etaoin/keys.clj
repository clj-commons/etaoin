(ns etaoin.keys
  "https://www.w3.org/TR/webdriver/#keyboard-actions")

(def unidentified       \uE000)
(def cancel             \uE001)
(def help               \uE002)
(def backspace          \uE003)
(def tab                \uE004)
(def clear              \uE005)
(def return             \uE006)
(def enter              \uE007)
(def shift-left         \uE008)
(def control-left       \uE009)
(def alt-left           \uE00A)
(def pause              \uE00B)
(def escape             \uE00C)
(def space              \uE00D)
(def pageup             \uE00E)
(def pagedown           \uE00F)
(def end                \uE010)
(def home               \uE011)
(def arrow-left         \uE012)
(def arrow-up           \uE013)
(def arrow-right        \uE014)
(def arrow-down         \uE015)
(def insert             \uE016)
(def delete             \uE017)
(def semicolon          \uE018)
(def equal              \uE019)
(def num-0              \uE01A)
(def num-1              \uE01B)
(def num-2              \uE01C)
(def num-3              \uE01D)
(def num-4              \uE01E)
(def num-5              \uE01F)
(def num-6              \uE020)
(def num-7              \uE021)
(def num-8              \uE022)
(def num-9              \uE023)
(def num-*              \uE024)
(def num-+              \uE025)
(def num-comma          \uE026)
(def num--              \uE027)
(def num-.              \uE028)
(def num-slash          \uE029)
(def f1                 \uE031)
(def f2                 \uE032)
(def f3                 \uE033)
(def f4                 \uE034)
(def f5                 \uE035)
(def f6                 \uE036)
(def f7                 \uE037)
(def f8                 \uE038)
(def f9                 \uE039)
(def f10                \uE03A)
(def f11                \uE03B)
(def f12                \uE03C)
(def meta-left          \uE03D)
(def zenkakuhankaku     \uE040)
(def shift-right        \uE050)
(def control-right      \uE051)
(def alt-right          \uE052)
(def meta-right         \uE053)
(def num-page-up        \uE054)
(def num-page-down      \uE055)
(def num-end            \uE056)
(def num-home           \uE057)
(def num-arrow-left     \uE058)
(def num-arrow-up       \uE059)
(def num-arrow-right    \uE05A)
(def num-arrow-down     \uE05B)
(def num-insert         \uE05C)
(def num-delete         \uE05D)

(def command            meta-left)


(defn chord
  [text & more]
  (str (apply str text more) unidentified))


(def with-shift
  (partial chord shift-left))

(def with-ctrl
  (partial chord control-left))

(def with-alt
  (partial chord alt-left))

(def with-command
  (partial chord command))


;;
;; Mouse codes
;;

(def mouse-left 0)
(def mouse-middle 1)
(def mouse-right 2)
