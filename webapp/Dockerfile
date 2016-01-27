FROM clojure

RUN mkdir -p /tmp/app
COPY . /tmp/app
WORKDIR /tmp/app
RUN lein deps

EXPOSE 8080

CMD lein repl :headless :host 0.0.0.0 :port 7888