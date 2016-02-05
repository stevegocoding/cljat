FROM clojure

RUN mkdir -p /tmp/app
COPY project.clj /tmp/app
WORKDIR /tmp/app
RUN lein deps

# ring web server
EXPOSE 3000

# nrepl 
EXPOSE 7888

# figwheel nrepl
EXPOSE 3449

# start ring embedded server
# CMD lein with-profile dev ring server-headless
CMD lein with-profile dev repl :headless