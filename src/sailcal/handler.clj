(ns sailcal.handler
  (:require
   [nrepl.server :as nrepl]
   [compojure.core :as cpj]
   [compojure.route :as cpjroute]
   [ring.middleware.defaults :as ring]
   [net.cgrand.enlive-html :as enlive]
   [clj-time.core :as time]
   [clj-time.format :as ftime]
   [clj-time.coerce :as ctime]
   [clojure.java.jdbc :as jdbc]
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [monger.core :as mg]
   [monger.credentials :as mcr]
   [monger.collection :as mc]
   [monger.operators :refer :all]
   ))

;;;
;;; Static parameters
;;;

(def timezone "America/Los_Angeles")
(def gmaps-key (System/getenv "GMAPS_KEY"))

;;;
;;; SQL database of reservations
;;;

;;; MySQL database spec for OpenShift or local

(def dbspec
  (if-let [host (System/getenv "MYSQL_SERVICE_HOST")]
;    {:connection-uri "mysql://172.30.87.81:3306/mysrsv?user=mystique&password=mystique"}
;    {:connection-uri "mysql://172.30.87.81:3306/mysrsv?user=mystique&password=mystique&useSSL=false"}
;    {:connection-uri "jdbc:mysql://172.30.32.217:3306/mysrsv?user=mystique&password=mystique&verifyServerCertificate=false&useSSL=true&requireSSL=true&?enabledTLSProtocols=TLSv1.2"}    
;    {:connection-uri "mysql://172.30.87.81:3306/mysrsv?user=mystique&password=mystique&verifyServerCertificate=false&useSSL=true&requireSSL=true&?enabledTLSProtocols=TLSv1.2"}
; the below worked with both mysql-connector-java 5.1.40 and 8.0.
;    {:connection-uri "jdbc:mysql://172.30.32.217:3306/mysrsv?user=mystique&password=mystique&useSSL=false"}
    {:connection-uri
     (str 
      "jdbc:mysql://"
      host ":"
      (System/getenv "MYSQL_SERVICE_PORT") "/"
      (System/getenv "SLCAL_SQLDB")
      "?user=" (System/getenv "SLCAL_SQLUSR")
      "&password=" (System/getenv "SLCAL_SQLPWD")
      "&useSSL=false")
     }
;    {:subtype "mysql"
;     :subname (str
;               "//"
;               host
;               ":"
;               (System/getenv "MYSQL_SERVICE_PORT")
;               "/"
;               (System/getenv "SLCAL_SQLDB"))
;     :user (System/getenv "SLCAL_SQLUSR")
;     :password (System/getenv "SLCAL_SQLPWD")
;     :useSSL "false"
;     }
;     :dbname (System/getenv "SLCAL_SQLDB")
;     :verifyServerCertificate "false"
;     :useSSL "true"
;     :requireSSL "true"
;     :enabledTLSProtocols "TLSv1.2"
;     }
    {:dbtype "mysql"
     :dbname (System/getenv "SLCAL_SQLDB")
     :subname (str
               "//localhost:3306/"
               (System/getenv "SLCAL_SQLDB"))
     :user (System/getenv "SLCAL_SQLUSR")
     :password (System/getenv "SLCAL_SQLPWD")
     }
    ))

; for testing in nREPL
;(def dbspec {:connection-uri "jdbc:mysql://172.30.22.141:3306/mysrsv?user=mystique&password=mystique&verifyServerCertificate=false&useSSL=true&requireSSL=true"})
;(jdbc/query dbspec ["select distinct date from reservations where date >= 2022-01-01"])

;;;
;;; MongoDB database of sailing tracks
;;;

;;; Mongo connection and db objects

(def mgconn 
  (if-let [host (System/getenv "MONGODB_SERVICE_HOST")]
    (let [port (Integer/parseInt
                (System/getenv
                 "MONGODB_SERVICE_PORT"))
          uname (System/getenv "SLCAL_MGUSR")
          dbname (System/getenv "SLCAL_MGDB")
          pwd-raw (System/getenv "SLCAL_MGPWD")
          pwd (.toCharArray pwd-raw)
          creds (mcr/create uname dbname pwd)]
      (mg/connect-with-credentials host port creds))
    (mg/connect)
    ))

(def mgdb (mg/get-db mgconn (System/getenv "SLCAL_MGDB")))


;;;
;;; Date/Time utilities
;;;

(defn sqldtobj-to-dtobj [sqldtobj]
  (time/to-time-zone
   (ctime/from-sql-time sqldtobj)
   (time/time-zone-for-id timezone)))


;;;
;;; Database read functions
;;;

; reservations and other 'events' from SQL database

(defn db-read-dtobjs [table start-dtstr end-dtstr]
  (map (fn [x]
         (sqldtobj-to-dtobj (:date x)))
       (jdbc/query dbspec
                   [(str
                     "SELECT DISTINCT date "
                     "FROM " table
                     " WHERE date >= \""
                     start-dtstr
                     "\" AND date <= \""
                     end-dtstr "\""
                     )
                    ])))

; sailing tracks from MongoDB
; fix to filter date >= datestr; currently returns all tracks ever

(defn trdb-read-dtobjs [coll start-dtstr end-dtstr]
  (map (fn [x] (:date x))
       (mc/find-maps mgdb coll))
  )

;;;
;;; Enlive - Clojure HTML templating
;;;

;;; for index, simply show index.html with vars replaced

(enlive/deftemplate index "sailcal/index.html"
  []
  [:#gmap] (enlive/replace-vars {:gmapskey gmaps-key})
  )

;;;
;;; Handlers for Compojure - Clojure web app routing
;;;

;;; main calendar page - enlive displays index.html SPA

(defn handler-get-index []
  (index)
  )

(defn handler-get-track [params]
  (let [trdate (get params "date")
        rawTrack (mc/find-one-as-map mgdb "tracks"
                                     {:date trdate})
        ]
    (if rawTrack
      (json/write-str (:points rawTrack))
      (json/write-str '[]))
    )
  )

;;; REST API for AJAX call to get dates as JSON
;;; returns array of e.g. {:title "Boat Rsvd" :start "2021-12-10"}

(defn handler-get-events [params]
  (let [start (get params "start" "2021-12-01")
        end (get params "end" "2021-12-31")]
    (json/write-str
     (concat
      (map (fn [x]
             {:title "Boat Reserved",
              :start (ftime/unparse
                      (ftime/formatters
                       :date)
                      x)
              })
           (db-read-dtobjs "reservations" start end))
; example of other types of events currently not being captured
;      (map (fn [x]
;             {:title "Bareboat",
;              :start (ftime/unparse
;                      (ftime/formatters
;                       :date)
;                      x)
;              })
;           (db-read-dtobjs "bareboat" start end))
      (map (fn [x]
             {:title "Track",
              :start x
              })
           (trdb-read-dtobjs "tracks" start end))))
    ))

;;;
;;; Compojure Routing
;;;

(cpj/defroutes app-routes
  (cpj/HEAD "/" [] "")
  (cpj/GET "/" []
    (handler-get-index))
  (cpj/GET "/track" {params :query-params}
    (handler-get-track params))
  (cpj/GET "/events" {params :query-params}
    (handler-get-events params))
  (cpjroute/files "/")
  (cpjroute/resources "/")
  (cpjroute/not-found "Not found.")
  )

;; start nREPL server

(defonce server (nrepl/start-server :port 7888))

;; generated by 'lein ring new'

(def app
  (ring/wrap-defaults app-routes
                      (assoc ring/site-defaults :proxy true)))

;;; EOF