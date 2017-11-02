(ns sync-bc.parser)
(import java.util.UUID)
(import java.time.format.DateTimeFormatter)
(import java.time.LocalDateTime)

(defn parse [s]
  (loop [done []
         todo (.split (.trim s) "\n")]
    (if (not-empty todo)
      (let [[date time lesson & rest] (drop-while #(not (.contains % ",")) todo)
            date (second (.split date ", "))
            time (first (.split time " "))
            [_ teacher & rest] (drop-while #(not (re-find #"\d" %)) rest)]
        (recur (conj done [date time lesson teacher]) rest))
      done)))

(defn format-event [m]
  (format "BEGIN:VEVENT\n%sEND:VEVENT\n"
          (apply str (for [[k v] m]
                       (str k ":" v "\n")))))

(defn spit-cal [ms]
  (spit "resources/out.ics"
        (format "%s%sEND:VCALENDAR"
                (slurp "resources/header.txt")
                (apply str (map format-event ms)))))

(def parser (DateTimeFormatter/ofPattern "d MMM HH:mm yyyy"))
(def formatter (DateTimeFormatter/ofPattern "yyyyMMdd'T'kkmm00"))

(defn process-point [[date time lesson teacher]]
  (let [year (if (or (.endsWith date "Nov") (.endsWith date "Dec")) 2017 2018)
        time (LocalDateTime/parse (str date " " time " " year) parser)]
    {"CREATED" "20171102T123322Z"
     "UID" (str (UUID/randomUUID))
     "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR" "AUTOMATIC"
     "SEQUENCE" "0"
     "DTEND;TZID=Asia/Ho_Chi_Minh" (.format (.plusMinutes time 90) formatter)
     "TRANSP" "OPAQUE"
     "SUMMARY" (str lesson " with " teacher)
     "DTSTART;TZID=Asia/Ho_Chi_Minh" (.format time formatter)}))

(->> (slurp "resources/data.txt")
     parse
     (map process-point)
     spit-cal)
