# sailcal

Example OpenShift Clojure+JavaScript+SQL+Mongo app with calendar of sailboat reservations and tracks of past voyages. Frontend is JavaScript single-page app (SPA) with two main divs: a calendar based on Fullcalendar.io and a map based on Google Maps. Calendar shows history and future of dates on which sailboat is reserved based on data scren-scraped from reservation website. Calendar also shows past datess for which track data is available. Clicking a date with "Track" indicated causes map div to display track. Works with companion utilities 'sailrsv' to get reservation data and 'sailtrk' to get track data.

```
git clone https://github.com/mpiech/sailcal
cd sailcal

oc project myproj
oc import-image mpiech/s2i-clojure --confirm # from Docker Hub
# first time build
oc new-build mpiech/s2i-clojure~. --name=sailcal --env-file=env.cfg
# subsequent rebuilds
oc start-build sailcal --from-dir=. --follow

oc new-app sailcal --env-file=env.cfg
oc expose svc/myscal --port=8080

############################################################
# env.cfg should specify the following environment variables

RSVDB=
TRKDB=
GMAPS_KEY=
ATLAS_HOST=
ATLAS_USERNAME=
ATLAS_PASSWORD=
ATLAS_DB=
PGHOST=
PGUSER=
PGPASSWORD=
PGDB=
SLCAL_SQLUSR=
SLCAL_SQLPWD=
SLCAL_SQLDB=
SLCAL_MGUSR=
SLCAL_MGPWD=
SLCAL_MGDB=
```

## Background

This project was developed on OpenShift using Clojure with Leiningen, Compojure, Enlive, JDBC, Monger, Emacs, Cider, nREPL, and others. Some notes regarding Clojure on OpenShift are in https://cloud.redhat.com/blog/using-clojure-on-openshift and https://cloud.redhat.com/blog/clojure-s2i-builder-openshift.

## License

Copyright © 2014-2022

Distributed under the Eclipse Public License version 1.0 or later.
