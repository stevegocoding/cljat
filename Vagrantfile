# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|

  # Docker Containers
  config.vm.define "cljat-webapp-dev" do |c|

    c.ssh.username = 'docker'
    c.ssh.password = 'tcuser'
    c.ssh.insert_key = true
    
    # Sync folder from vagrant host to container
    # c.vm.synced_folder ".", "/tmp/app", type: "rsync"
    
    c.vm.provider "docker" do |d|
      
      # Dockerfile build directory
      d.build_dir = "."
      d.build_args = ["-t", "cltat-webapp-nrepl-img"]

      # Args for docker run
      d.create_args = ["-v=/tmp/app:/tmp/app"]
      
      # Container  name
      d.name = "cljat-webapp-dev-container"
      
      # Port forwarding [docker-host-vm:container]
      d.ports = ["55555:7888", # clojure nRepl
                 "55556:3449", # figwheel nRepl
                 "8080:3000"   # ring web server
                ]

      d.remains_running = true

      # Mount
      # d.volumes = ["/tmp/app/:/tmp/app/"]

      # custom docker-host vm
      d.vagrant_vagrantfile = "./Vagrantfile.proxy"
      
    end
  end
end
