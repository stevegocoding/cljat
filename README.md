# webapp

The web-tier of the cljat application

## Usage

### Setup Development Environment with Docker
* Prepare the dependencies volumes container  
```
cd webapp && lein deps
```	     
```	
docker run -d --name lein_dev_deps -v ~/.lein:/root/.lein -v ~/.m2:/root/.m2 busybox
```

* Runing the dev container in which an nRepl server is running
ports forwarding:     
	web server: 8080 -> 8080   
	nRepl server: 55555 -> 7888
  figwhell server: 8081 -> 8081
  figwheel nRepl server: 55556 -> 7889
```
docker run --rm -t --volumes-from=lein_dev_deps -v $(pwd):/webapp -p 8080:8080 -p 55555:7888 -p 8081:8081 -p 55556:7889 cljat-webapp-nrepl-img
``` 

* Connect to nRepl server with Emacs/Cider   
cider-connect to $(docker-machine ip):55555



## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
