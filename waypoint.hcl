project = "pro-sante-connect"

# Labels can be specified for organizational purposes.
labels = { "domaine" = "psc" }

runner {
    enabled = true
    data_source "git" {
        url = "https://github.com/prosanteconnect/pscload.git"
    }
}

# An application to deploy.
app "prosanteconnect/pscload" {
    # Build specifies how an application should be deployed. In this case,
    # we'll build using a Dockerfile and keeping it in a local registry.
    build {
        use "pack" {}

        # Uncomment below to use a remote docker registry to push your built images.
        #
        registry {
           use "docker" {
             image = "prosanteconnect/pscload"
             tag   = "wp"
             encoded_auth = filebase64("/secrets/dockerAuth.json")
           }
        }
    }

    # Deploy to Nomad
    deploy {
      use "nomad-jobspec" {    
        jobspec = "${path.app}/pscload.nomad"
      }
    }
}
