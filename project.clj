(defproject sailcal "2.1.1"
  :description "Sailboat Reservation and Tracks Calendar"
  :url "http://piech.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies
  [
   ;http://clojure.org/downloads
   [org.clojure/clojure "1.10.0"]
   ;https://github.com/nrepl/nrepl
   [nrepl "0.9.0"]
   ;https://github.com/weavejester/compojure
   [compojure "1.6.2"]
   ;https://github.com/cgrand/enlive
   [enlive "1.1.6"]
   ;https://github.com/ring-clojure/ring
   [ring/ring-defaults "0.3.2"]
   ;https://github.com/clojure/data.json/
   [org.clojure/data.json "2.4.0"]
   ;https://github.com/clj-time/clj-time
   [clj-time "0.15.2"]
   ;https://github.com/clojure/java.jdbc
   [org.clojure/java.jdbc "0.7.12"]
   ;http://dev.mysql.com/downloads/connector/j/
   [mysql/mysql-connector-java "8.0.28"]
   ;https://github.com/michaelklishin/monger
   [com.novemberain/monger "3.5.0"]
   ;https://github.com/remodoy/clj-postgresql
   [org.postgresql/postgresql "42.3.1"]
   ]
  :plugins [[lein-ring "0.12.6"]
            [cider/cider-nrepl "0.27.3"]]
  :ring {:handler sailcal.handler/app
         :port 8080
         }
  :profiles
  {:dev {:dependencies [[javax.servlet/javax.servlet-api "4.0.1"]
                        [ring/ring-mock "0.4.0"]]}}
  )
